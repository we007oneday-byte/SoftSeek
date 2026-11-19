package io.github.we007oneday_byte.softseek.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.we007oneday_byte.softseek.domain.repository.MemoryPoolRepository

@HiltWorker
class DeletedPoolCleanupWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val memoryPoolRepository: MemoryPoolRepository,
    ) : CoroutineWorker(appContext, workerParams) {
        override suspend fun doWork(): Result = try {
            val purged = memoryPoolRepository.purgeExpired()
            Result.success(workDataOf(KEY_PURGED_COUNT to purged))
        } catch (exception: Exception) {
            Result.retry()
        }

        companion object {
            const val UNIQUE_WORK_NAME = "softseek_purge_deleted_pools"
            const val KEY_PURGED_COUNT = "purged_count"
        }
    }
