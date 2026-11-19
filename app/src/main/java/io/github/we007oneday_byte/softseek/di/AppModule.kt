package io.github.we007oneday_byte.softseek.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.we007oneday_byte.softseek.core.model.AppInfo
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppInfo(
        @ApplicationContext context: Context,
    ): AppInfo {
        @Suppress("DEPRECATION")
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return AppInfo(
            packageName = context.packageName,
            versionName = packageInfo.versionName ?: "unknown",
        )
    }
}
