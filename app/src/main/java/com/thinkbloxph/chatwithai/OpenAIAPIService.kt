package com.thinkbloxph.chatwithai

import com.google.gson.JsonObject
import com.thinkbloxph.chatwithai.network.model.TranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

interface OpenAIAPIService {
    @Headers("Content-Type: application/json")
    @POST("/v1/chat/completions")
    fun getCompletion(
        @Header("Authorization") authorization: String,
        @Body body: JsonObject
    ): Call<JsonObject>

    @Multipart
    @POST("/v1/audio/transcriptions")
    fun getTranscription(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody?
    ): Call<TranscriptionResponse>
}
