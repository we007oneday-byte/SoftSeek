package io.github.we007oneday_byte.softseek.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.we007oneday_byte.softseek.core.constant.AppConstants
import io.github.we007oneday_byte.softseek.core.network.BaseUrlValidator
import io.github.we007oneday_byte.softseek.data.remote.DeepSeekApi
import io.github.we007oneday_byte.softseek.data.remote.DeepSeekStreamApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideDeepSeekApi(
        json: Json,
        client: OkHttpClient,
    ): DeepSeekApi = retrofit(json, client).create(DeepSeekApi::class.java)

    @Provides
    @Singleton
    fun provideDeepSeekStreamApi(
        json: Json,
        client: OkHttpClient,
    ): DeepSeekStreamApi {
        val streamClient = client.newBuilder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
        return retrofit(json, streamClient).create(DeepSeekStreamApi::class.java)
    }

    private fun retrofit(json: Json, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BaseUrlValidator.validate(AppConstants.DEFAULT_BASE_URL))
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
