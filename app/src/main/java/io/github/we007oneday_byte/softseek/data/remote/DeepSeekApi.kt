package io.github.we007oneday_byte.softseek.data.remote

import io.github.we007oneday_byte.softseek.data.remote.dto.BalanceResponseDto
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

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequestDto,
    ): Response<ChatCompletionResponseDto>

    @GET("models")
    suspend fun listModels(
        @Header("Authorization") authorization: String,
    ): Response<ModelsResponseDto>

    @GET("user/balance")
    suspend fun balance(
        @Header("Authorization") authorization: String,
    ): Response<BalanceResponseDto>
}

interface DeepSeekStreamApi {
    @Streaming
    @POST("chat/completions")
    suspend fun streamChat(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequestDto,
    ): Response<ResponseBody>
}
