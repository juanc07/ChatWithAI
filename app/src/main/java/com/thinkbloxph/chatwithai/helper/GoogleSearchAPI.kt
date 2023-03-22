package com.thinkbloxph.chatwithai.helper

import android.content.Intent
import android.net.Uri
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.thinkbloxph.chatwithai.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.QueryMap
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

interface GoogleSearchAPIService {
    @GET("customsearch/v1")
    fun search(@QueryMap query: Map<String, String>): Call<SearchResult>
}

data class SearchResult(
    @SerializedName("items") val items: List<SearchItem>
)

data class SearchItem(
    @SerializedName("title") val title: String,
    @SerializedName("link") val link: String,
    @SerializedName("size") val size: Long,
    @SerializedName("snippet") val snippet: String
)

private const val INNER_TAG = "GoogleSearchAPI"
class GoogleSearchAPI private constructor() {

    companion object {
        private val instance = GoogleSearchAPI()
        fun getInstance() = instance
    }

    private val googleSearchAPIService: GoogleSearchAPIService by lazy {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()
            .create(GoogleSearchAPIService::class.java)
    }

    /*suspend fun search(query: String, apiKey: String, cx: String, numResults: Int): String {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        val queryMap = mapOf(
            "q" to encodedQuery,
            "key" to apiKey,
            "cx" to cx,
            "num" to numResults.toString()
        )

        return withContext(Dispatchers.IO) {
            try {
                val call = googleSearchAPIService.search(queryMap)
                val response = call.execute()

                if (response.isSuccessful) {
                    val searchResult = response.body()
                    val sb = StringBuilder()
                    sb.append("Here are the top $numResults search results for \"$query\":\n\n")
                    for (item in searchResult?.items ?: emptyList()) {
                        sb.append("Title: ${item.title}\n")
                        val url = item.link
                        val spannable = SpannableString("URL: $url\n\n")
                        spannable.setSpan(
                            object : ClickableSpan() {
                                override fun onClick(widget: View) {
                                    // Open the link in the browser when the link is clicked
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    widget.context.startActivity(intent)
                                }
                            },
                            5, 5 + url.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        sb.append(spannable)
                    }
                    sb.toString()
                } else {
                    "Sorry, something went wrong while searching for \"$query\"."
                }
            } catch (e: Exception) {
                "Sorry, something went wrong while searching for \"$query\"."
            }
        }
    }*/

    /*suspend fun searchGoogle(query: String, apiKey: String, cx: String, numResults: Int): List<Pair<String, String>> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        val queryMap = mapOf(
            "q" to encodedQuery,
            "key" to apiKey,
            "cx" to cx,
            "num" to numResults.toString()
        )

        return withContext(Dispatchers.IO) {
            try {
                val call = googleSearchAPIService.search(queryMap)
                val response = call.execute()

                if (response.isSuccessful) {
                    Log.d(TAG, "[INNER_TAG}]: find something success!!")
                    val searchResult = response.body()
                    val results = mutableListOf<Pair<String, String>>()
                    for (item in searchResult?.items ?: emptyList()) {
                        val title = item.title ?: ""
                        val url = item.link ?: ""
                        results.add(Pair(title, url))
                    }
                    results
                } else {
                    Log.d(TAG, "[INNER_TAG}]: empty didn't find anything!!")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.d(TAG, "[INNER_TAG}]: Exception: ${e.toString()}")
                emptyList()
            }
        }
    }*/

    suspend fun searchGoogle(query: String, apiKey: String, cx: String, numResults: Long): List<Triple<String, String, String>> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        val queryMap = mapOf(
            "q" to encodedQuery,
            "key" to apiKey,
            "cx" to cx,
            "num" to numResults.toString(),
            "fields" to "items(title,link,snippet)"
        )

        return withContext(Dispatchers.IO) {
            try {
                val call = googleSearchAPIService.search(queryMap)
                val response = call.execute()

                if (response.isSuccessful) {
                    Log.d(TAG, "[INNER_TAG}]: find something success!!")
                    val searchResult = response.body()
                    val results = mutableListOf<Triple<String, String, String>>()
                    for (item in searchResult?.items ?: emptyList()) {
                        val title = item.title ?: ""
                        val url = item.link ?: ""
                        val snippet = item.snippet ?: ""
                        results.add(Triple(title, url, snippet))
                    }
                    results
                } else {
                    Log.d(TAG, "[INNER_TAG}]: empty didn't find anything!!")
                    emptyList()
                }
            } catch (e: Exception) {
                Log.d(TAG, "[INNER_TAG}]: Exception: ${e.toString()}")
                emptyList()
            }
        }
    }

    fun makeClickableUrls(result: Triple<String, String, String>): SpannableString {
        val title = result.first
        val url = result.second
        val snippet = result.third

        // Create a SpannableString with the title, snippet, and URL separated by newline characters
        val spannableString = SpannableString("$title\n$snippet\n$url")

        // Create a clickable span for the URL
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                widget.context.startActivity(intent)
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
            }
        }

        // Set the clickable span for the URL
        spannableString.setSpan(
            clickableSpan,
            title.length + snippet.length + 2, // Add 2 for the newline characters
            spannableString.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return spannableString
    }


    fun containsSearchKeyword(input: String): Boolean {
        val keywords = listOf("search", "look", "research")
        val regex = "\\b(${keywords.joinToString("|")})\\b".toRegex(RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(input)
    }

}
