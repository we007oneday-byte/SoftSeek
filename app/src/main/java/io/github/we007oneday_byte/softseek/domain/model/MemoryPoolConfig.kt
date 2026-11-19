package io.github.we007oneday_byte.softseek.domain.model

data class MemoryPoolConfig(
    val poolId: String,
    val maxInputTokens: Int,
    val enableAutoCleanup: Boolean,
    val cleanupThresholdCount: Int,
    val cleanupProtectionHours: Int,
    val deletedRetentionDays: Int,
    val tokenEstimateRatio: Double,
    val callAutoDowngrade: Boolean,
    val imageDetail: ImageDetail,
    val updatedAt: Long,
) {
    companion object {
        const val DEFAULT_MAX_INPUT_TOKENS = 32_768
        const val DEFAULT_CLEANUP_THRESHOLD_COUNT = 2
        const val DEFAULT_CLEANUP_PROTECTION_HOURS = 24
        const val DEFAULT_DELETED_RETENTION_DAYS = 7
        const val DEFAULT_TOKEN_ESTIMATE_RATIO = 1.5
        const val DEFAULT_IMAGE_TOKENS = 384

        fun defaults(poolId: String, updatedAt: Long): MemoryPoolConfig = MemoryPoolConfig(
            poolId = poolId,
            maxInputTokens = DEFAULT_MAX_INPUT_TOKENS,
            enableAutoCleanup = false,
            cleanupThresholdCount = DEFAULT_CLEANUP_THRESHOLD_COUNT,
            cleanupProtectionHours = DEFAULT_CLEANUP_PROTECTION_HOURS,
            deletedRetentionDays = DEFAULT_DELETED_RETENTION_DAYS,
            tokenEstimateRatio = DEFAULT_TOKEN_ESTIMATE_RATIO,
            callAutoDowngrade = true,
            imageDetail = ImageDetail.AUTO,
            updatedAt = updatedAt,
        )
    }
}
