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
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File


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

    suspend fun getCompletion(message:String,prompt:String,prevMessage:String?,gptToken:String): List<String> {
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

                val call = openAIAPIService.getCompletion(
                    "Bearer $gptToken",
                    json
                )

               // val call = openAIAPIService.getCompletion(json)
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

    suspend fun summarizeText(message:String,gptToken:String): List<String> {
        val commandMsg = "Please summarize the following text:\n$message"
        Log.d(TAG, "[INNER_TAG}]: commandMsg: ${commandMsg}")
        val json = JsonObject().apply {
            addProperty("model", "gpt-3.5-turbo")
            addProperty("temperature", 0.5)
            addProperty("max_tokens", 60)
            addProperty("top_p", 1.0)
            addProperty("frequency_penalty", 0.0)
            addProperty("presence_penalty", 0.0)

            add("messages", JsonArray().apply {
                add(JsonObject().apply {
                    addProperty("role", "system")
                    addProperty("content", "Return only the main response. remove the pre-text and post-text")
                })

                add(JsonObject().apply {
                    addProperty("role", "user")
                    addProperty("content", commandMsg)
                })
            })
        }

        return withContext(Dispatchers.IO) {
            try {
                //val call = openAIAPIService.getCompletion(json)
                //val call = openAIAPIService.getCompletion(json)

                val call = openAIAPIService.getCompletion(
                    "Bearer $gptToken",
                    json
                )
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

    suspend fun transcribeAudio(file: File, gptToken: String): List<String> {
        Log.d(TAG, "[INNER_TAG}]: transcribeAudio file: ${file}")
        Log.d(TAG, "[INNER_TAG}]: transcribeAudio gptToken: ${gptToken}")
        // Create request body
        val requestBody = "whisper-1".toRequestBody()
        //val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        //val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
        val filePart = MultipartBody.Part.createFormData("file", file.name, file.asRequestBody())

        return withContext(Dispatchers.IO) {
            try {
                val call = openAIAPIService.getTranscription(
                    "Bearer $gptToken",
                    filePart,
                    requestBody
                )
                val response = call.execute()

                if (response.isSuccessful) {
                    val result = response.body()
                    val transcripts = result?.text
                    transcripts?.let { listOf(it) } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: IOException) {
                Log.d(TAG, "[INNER_TAG}]: transcribeAudio error: ${e.toString()}")
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

