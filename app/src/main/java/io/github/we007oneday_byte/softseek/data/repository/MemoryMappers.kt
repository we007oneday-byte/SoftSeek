package io.github.we007oneday_byte.softseek.data.repository

import io.github.we007oneday_byte.softseek.data.local.entity.MemoryHeapNodeEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryHeapNodeWithAttachments
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolBlockEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolConfigEntity
import io.github.we007oneday_byte.softseek.data.local.entity.MessageAttachmentEntity
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.Conversation
import io.github.we007oneday_byte.softseek.domain.model.MemoryPoolConfig
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment

internal fun MemoryPoolBlockEntity.toDomain(): Conversation = Conversation(
    id = poolId,
    title = title,
    systemPrompt = systemPrompt,
    modelName = modelName,
    reasoningEffort = reasoningEffort,
    maxOutputTokens = maxOutputTokens,
    tags = tags,
    isPinned = isPinned,
    isDeleted = isDeleted,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastMessageAt = lastMessageAt,
    lastMessagePreview = lastMessagePreview,
    deletedAt = deletedAt,
)

internal fun MemoryHeapNodeWithAttachments.toDomain(): ChatMessage = ChatMessage(
    id = node.nodeId,
    conversationId = node.poolId,
    parentId = node.parentNodeId,
    role = node.role,
    content = node.content,
    reasoningContent = node.reasoningContent,
    reasoningTokens = node.reasoningTokens,
    modelName = node.modelName,
    status = node.status,
    finishReason = node.finishReason,
    errorMessage = node.errorMessage,
    promptTokens = node.promptTokens,
    completionTokens = node.completionTokens,
    totalTokens = node.totalTokens,
    timestamp = node.timestamp,
    isDeleted = node.isDeleted,
    deletedAt = node.deletedAt,
    attachments = attachments.map { it.toDomain() },
)

internal fun ChatMessage.toEntity(): MemoryHeapNodeEntity = MemoryHeapNodeEntity(
    nodeId = id,
    poolId = conversationId,
    parentNodeId = parentId,
    role = role,
    content = content,
    reasoningContent = reasoningContent,
    reasoningTokens = reasoningTokens,
    modelName = modelName,
    status = status,
    finishReason = finishReason,
    errorMessage = errorMessage,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
    timestamp = timestamp,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
)

internal fun MessageAttachmentEntity.toDomain(): MessageAttachment = MessageAttachment(
    attachmentId = attachmentId,
    nodeId = nodeId,
    type = type,
    localPath = localPath,
    mime = mime,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    sortOrder = sortOrder,
)

internal fun MessageAttachment.toEntity(): MessageAttachmentEntity = MessageAttachmentEntity(
    attachmentId = attachmentId,
    nodeId = nodeId,
    type = type,
    localPath = localPath,
    mime = mime,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    sortOrder = sortOrder,
)

internal fun MemoryPoolConfigEntity.toDomain(): MemoryPoolConfig = MemoryPoolConfig(
    poolId = poolId,
    maxInputTokens = maxInputTokens,
    enableAutoCleanup = enableAutoCleanup,
    cleanupThresholdCount = cleanupThresholdCount,
    cleanupProtectionHours = cleanupProtectionHours,
    deletedRetentionDays = deletedRetentionDays,
    tokenEstimateRatio = tokenEstimateRatio,
    callAutoDowngrade = callAutoDowngrade,
    imageDetail = imageDetail,
    updatedAt = updatedAt,
)

internal fun MemoryPoolConfig.toEntity(): MemoryPoolConfigEntity = MemoryPoolConfigEntity(
    poolId = poolId,
    updatedAt = updatedAt,
    maxInputTokens = maxInputTokens,
    enableAutoCleanup = enableAutoCleanup,
    cleanupThresholdCount = cleanupThresholdCount,
    cleanupProtectionHours = cleanupProtectionHours,
    deletedRetentionDays = deletedRetentionDays,
    tokenEstimateRatio = tokenEstimateRatio,
    callAutoDowngrade = callAutoDowngrade,
    imageDetail = imageDetail,
)
