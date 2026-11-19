package io.github.we007oneday_byte.softseek.domain.model

data class CleanupCandidate(
    val nodeId: String,
    val role: MessageRole,
    val preview: String,
    val timestamp: Long,
)

data class CleanupResult(
    val poolId: String,
    val preview: Boolean,
    val candidates: List<CleanupCandidate>,
    val deletedCount: Int,
    val skippedReason: CleanupSkipReason? = null,
)

enum class CleanupSkipReason {
    AUTO_CLEANUP_DISABLED,
    POOL_PINNED,
    POOL_NOT_FOUND,
    NOTHING_TO_CLEAN,
}
