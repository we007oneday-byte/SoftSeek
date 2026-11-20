package io.github.we007oneday_byte.softseek.data.remote

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

/** Anthropic Messages API；@Url 使用绝对地址，Base URL 可包含或不包含 /v1。 */
interface AnthropicApi {
    @Streaming
    @POST
    suspend fun messages(
        @Url url: String,
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String,
        @Body body: RequestBody,
    ): Response<ResponseBody>
}
