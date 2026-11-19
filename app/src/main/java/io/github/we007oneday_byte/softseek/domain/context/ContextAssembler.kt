package io.github.we007oneday_byte.softseek.domain.context

import io.github.we007oneday_byte.softseek.domain.model.AttachmentType
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.ContextBuildResult
import io.github.we007oneday_byte.softseek.domain.model.ContextMessage
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.MessageStatus
import javax.inject.Inject

class ContextAssembler
    @Inject
    constructor(
        private val tokenEstimator: TokenEstimator,
    ) {
        fun assemble(
            systemPrompt: String?,
            history: List<ChatMessage>,
            maxInputTokens: Int,
            tokenEstimateRatio: Double = MemoryPoolConfig.DEFAULT_TOKEN_ESTIMATE_RATIO,
        ): ContextBuildResult {
            val budget = maxInputTokens.coerceAtLeast(0)
            var truncated = false
            val systemMessage = buildSystemMessage(systemPrompt, budget, tokenEstimateRatio)?.also {
                val promptWasTruncated = it.content.length < systemPrompt.orEmpty().length
                if (it.tokenEstimate >= budget && (history.isNotEmpty() || promptWasTruncated)) {
                    truncated = true
                }
            }
            var usedTokens = systemMessage?.tokenEstimate ?: 0
            val candidates = history
                .asSequence()
                .filter { it.role != MessageRole.SYSTEM }
                .filter { !it.isDeleted }
                .filter { it.status == MessageStatus.COMPLETE }
                .filter { it.content.isNotBlank() || it.attachments.isNotEmpty() }
                .sortedBy { it.timestamp }
                .toList()

            val selected = mutableListOf<ContextMessage>()
            for (message in candidates.asReversed()) {
                val imageAttachments = message.attachments.filter { it.type == AttachmentType.IMAGE }
                val imageCount = imageAttachments.size
                val imageTokens = tokenEstimator.estimateImages(imageCount)
                val remaining = budget - usedTokens
                if (remaining <= imageTokens) {
                    truncated = true
                    continue
                }
                val textBudget = remaining - imageTokens
                val fittedContent = tokenEstimator.truncateText(
                    text = message.content,
                    maxTokens = textBudget,
                    tokenEstimateRatio = tokenEstimateRatio,
                )
                if (fittedContent.length < message.content.length) {
                    truncated = true
                }
                val contentTokens = tokenEstimator.estimateText(fittedContent, tokenEstimateRatio)
                val totalTokens = contentTokens + imageTokens
                if (totalTokens > remaining) {
                    truncated = true
                    continue
                }
                selected.add(
                    0,
                    ContextMessage(
                        role = message.role,
                        content = fittedContent,
                        imageCount = imageCount,
                        tokenEstimate = totalTokens,
                        images = imageAttachments,
                    ),
                )
                usedTokens += totalTokens
                if (usedTokens >= budget) {
                    truncated = true
                    break
                }
            }

            val messages = buildList {
                if (systemMessage != null) add(systemMessage)
                addAll(selected)
            }
            return ContextBuildResult(
                messages = messages,
                estimatedInputTokens = usedTokens,
                truncated = truncated,
            )
        }

        private fun buildSystemMessage(
            systemPrompt: String?,
            budget: Int,
            tokenEstimateRatio: Double,
        ): ContextMessage? {
            if (systemPrompt.isNullOrBlank() || budget <= 0) return null
            val content = tokenEstimator.truncateText(systemPrompt, budget, tokenEstimateRatio)
            return ContextMessage(
                role = MessageRole.SYSTEM,
                content = content,
                imageCount = 0,
                tokenEstimate = tokenEstimator.estimateText(content, tokenEstimateRatio),
            )
        }
    }
