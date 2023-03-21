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
import android.util.Log


private const val INNER_TAG = "OpenAIAPI"
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

    /*
           addProperty("temperature", 0.5)
           addProperty("max_tokens", 60)
           addProperty("top_p", 1.0)
           addProperty("frequency_penalty", 0.0)
           addProperty("presence_penalty", 0.0)*/
    /*add("messages", JsonArray().apply {
        add(JsonObject().apply {
            addProperty("role", "user")
            addProperty("role", "system")
            addProperty("content", message)
        })
    })*/

    suspend fun getCompletion(message:String,prompt:String,prevMessage:String?): List<String> {
        Log.d(TAG, "[INNER_TAG}]: prompt: ${prompt}")
        val json = JsonObject().apply {
            addProperty("model", "gpt-3.5-turbo")

            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", prompt)
                })

                if(prevMessage != null) {
                    add(JsonObject().apply {
                        addProperty("role", "assistant")
                        addProperty("content", prevMessage)
                    })
                }

                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", message)
                })
            })
        }

        return withContext(Dispatchers.IO) {
            try {
                //val call = openAIAPIService.getCompletion(json)
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

    fun isSummaryLengthValid(summary: String): Boolean {
        val tokens = summary.split(" ")
        return tokens.size <= 2048
    }

    suspend fun summarizeText(text: String): String? {
        val prompt = "Please summarize the following text:\n$text"
        val json = JsonObject().apply {
            addProperty("model", "text-davinci-002")
            addProperty("temperature", 0.5)
            addProperty("max_tokens", 60)
            addProperty("top_p", 1.0)
            addProperty("frequency_penalty", 0.0)
            addProperty("presence_penalty", 0.0)
            add("prompt", JsonArray().apply {
                add(prompt)
            })
        }

        return withContext(Dispatchers.IO) {
            try {
                val call = openAIAPIService.getCompletion(json)
                val response = call.execute()

                if (response.isSuccessful) {
                    val result = response.body()
                    val choices = result?.getAsJsonArray("choices")
                    choices!!.first().asJsonObject.getAsJsonObject("text").asString
                } else {
                    null
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Error")
                        .setMessage("Something went wrong. Please try again later.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                null
            } catch (e: HttpException) {
                withContext(Dispatchers.Main) {
                    MaterialAlertDialogBuilder(context)
                        .setTitle("Error")
                        .setMessage("Something went wrong. Please try again later.")
                        .setPositiveButton("OK", null)
                        .show()
                }
                null
            }
        }
    }

}

