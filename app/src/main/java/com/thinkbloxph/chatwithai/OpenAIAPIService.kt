package com.thinkbloxph.chatwithai

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenAIAPIService {
    @Headers("Content-Type: application/json", "Authorization: Bearer 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")
    @POST("/v1/chat/completions")
    fun getCompletion(@Body body: JsonObject): Call<JsonObject>
}
