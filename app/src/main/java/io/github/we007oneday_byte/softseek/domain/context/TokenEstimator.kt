package io.github.we007oneday_byte.softseek.domain.context

import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.floor

class TokenEstimator
    @Inject
    constructor() {
        fun estimateText(
            text: String,
            tokenEstimateRatio: Double = MemoryPoolConfig.DEFAULT_TOKEN_ESTIMATE_RATIO,
        ): Int {
            if (text.isBlank()) return 0
            val ratio = tokenEstimateRatio.coerceIn(MIN_RATIO, MAX_RATIO)
            return ceil(text.length / ratio).toInt()
        }

        fun estimateImages(
            imageCount: Int,
            imageTokens: Int = MemoryPoolConfig.DEFAULT_IMAGE_TOKENS,
        ): Int = imageCount.coerceAtLeast(0) * imageTokens

        fun estimateMessage(
            content: String,
            imageCount: Int,
            tokenEstimateRatio: Double = MemoryPoolConfig.DEFAULT_TOKEN_ESTIMATE_RATIO,
        ): Int = estimateText(content, tokenEstimateRatio) + estimateImages(imageCount)

        fun truncateText(
            text: String,
            maxTokens: Int,
            tokenEstimateRatio: Double = MemoryPoolConfig.DEFAULT_TOKEN_ESTIMATE_RATIO,
        ): String {
            if (maxTokens <= 0) return ""
            val ratio = tokenEstimateRatio.coerceIn(MIN_RATIO, MAX_RATIO)
            val maxChars = floor(maxTokens * ratio).toInt().coerceAtLeast(0)
            return if (text.length <= maxChars) text else text.take(maxChars)
        }

        fun calibrateRatio(
            currentRatio: Double,
            estimatedTokens: Int,
            actualTokens: Int,
        ): Double {
            if (estimatedTokens <= 0 || actualTokens <= 0) {
                return currentRatio.coerceIn(MIN_RATIO, MAX_RATIO)
            }
            return (currentRatio * estimatedTokens / actualTokens).coerceIn(MIN_RATIO, MAX_RATIO)
        }

        private companion object {
            const val MIN_RATIO = 1.0
            const val MAX_RATIO = 6.0
        }
    }
