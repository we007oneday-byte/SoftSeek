package io.github.we007oneday_byte.softseek.data.remote

import io.github.we007oneday_byte.softseek.data.remote.dto.ChatCompletionRequestDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ChatCompletionResponseDto
import io.github.we007oneday_byte.softseek.data.remote.dto.ModelsResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * OpenAI Chat Completions 兼容接口。兼容官方 OpenAI 以及所有实现
 * `POST /chat/completions`、`GET /models` 的第三方服务。
 */
interface OpenAiApi {
    @POST
    suspend fun chat(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequestDto,
    ): Response<ChatCompletionResponseDto>

    @GET
    suspend fun listModels(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<ModelsResponseDto>
}

interface OpenAiStreamApi {
    @Streaming
    @POST
    suspend fun streamChat(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequestDto,
    ): Response<ResponseBody>
}
