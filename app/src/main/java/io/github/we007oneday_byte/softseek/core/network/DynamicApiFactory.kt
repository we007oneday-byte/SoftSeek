package io.github.we007oneday_byte.softseek.core.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.github.we007oneday_byte.softseek.data.remote.DeepSeekApi
import io.github.we007oneday_byte.softseek.data.remote.DeepSeekStreamApi
import io.github.we007oneday_byte.softseek.data.settings.SettingsRepository
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 按当前设置构建 OkHttp/Retrofit；BaseURL 由 @Url 绝对地址覆盖，
 * 代理与超时在这里生效。设置变化时下一次请求自动重建。
 */
@Singleton
class DynamicApiFactory
    @Inject
    constructor(
        private val settingsRepository: SettingsRepository,
        private val json: Json,
    ) {
        private data class NetworkConfig(
            val baseUrl: String,
            val proxyUrl: String,
            val connect: Int,
            val read: Int,
            val write: Int,
        )

        @Volatile
        private var cachedConfig: NetworkConfig? = null

        @Volatile
        private var cachedApi: DeepSeekApi? = null

        @Volatile
        private var cachedStreamApi: DeepSeekStreamApi? = null

        fun api(): DeepSeekApi {
            ensure()
            return checkNotNull(cachedApi)
        }

        fun streamApi(): DeepSeekStreamApi {
            ensure()
            return checkNotNull(cachedStreamApi)
        }

        @Synchronized
        private fun ensure() {
            val settings = settingsRepository.settings.value
            val config = NetworkConfig(
                baseUrl = settings.baseUrl,
                proxyUrl = settings.proxyUrl,
                connect = settings.connectTimeoutSeconds,
                read = settings.readTimeoutSeconds,
                write = settings.writeTimeoutSeconds,
            )
            if (config == cachedConfig && cachedApi != null && cachedStreamApi != null) return
            val proxy = parseProxy(config.proxyUrl)
            val client = OkHttpClient.Builder()
                .connectTimeout(config.connect.toLong(), TimeUnit.SECONDS)
                .readTimeout(config.read.toLong(), TimeUnit.SECONDS)
                .writeTimeout(config.write.toLong(), TimeUnit.SECONDS)
                .apply { if (proxy != null) proxy(proxy) }
                .build()
            val streamClient = client.newBuilder()
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()
            cachedApi = retrofit(client).create(DeepSeekApi::class.java)
            cachedStreamApi = retrofit(streamClient).create(DeepSeekStreamApi::class.java)
            cachedConfig = config
        }

        private fun parseProxy(raw: String): Proxy? {
            val value = raw.trim()
            if (value.isEmpty()) return null
            val url = value.toHttpUrlOrNull() ?: return null
            val port = if (url.port > 0) url.port else 8080
            return Proxy(Proxy.Type.HTTP, InetSocketAddress(url.host, port))
        }

        private fun retrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
            .baseUrl(BaseUrlValidator.validate(settingsRepository.settings.value.baseUrl))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
