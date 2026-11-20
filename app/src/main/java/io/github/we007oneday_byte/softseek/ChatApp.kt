package io.github.we007oneday_byte.softseek

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.we007oneday_byte.softseek.core.logging.AppLog
import io.github.we007oneday_byte.softseek.data.worker.PurgeScheduler
import javax.inject.Inject

@HiltAndroidApp
class ChatApp : Application(), Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        AppLog.install(this, BuildConfig.VERSION_NAME)
        PurgeScheduler.schedule(this)
    }
}
