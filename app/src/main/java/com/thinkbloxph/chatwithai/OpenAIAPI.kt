package com.thinkbloxph.chatwithai

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import java.io.IOException
import retrofit2.HttpException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context

class OpenAIAPI(private val coroutineScope: CoroutineScope,private val context: Context) {

    private val openAIAPIService: OpenAIAPIService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.openai.com")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(OpenAIAPIService::class.java)
    }

    suspend fun getCompletion(message:String): List<String> {
        val json = JsonObject().apply {
            addProperty("model", "gpt-3.5-turbo")
            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", message)
                })
            })
        }

        return withContext(Dispatchers.IO) {
            try {
                val call = openAIAPIService.getCompletion(json)
                val response = call.execute()

                if (response.isSuccessful) {
                    val result = response.body()
                    val choices = result?.getAsJsonArray("choices")
                    choices!!.map { it.asJsonObject.getAsJsonObject("message").get("content").asString }
                } else {
                    emptyList()
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Error")
                        .setMessage("Something went wrong. Please try again later.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                emptyList()
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Error")
                        .setMessage("Something went wrong. Please try again later.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                emptyList()
            }
        }
    }
}

