import android.os.AsyncTask
import android.util.Log
import com.thinkbloxph.chatwithai.TAG
import com.thinkbloxph.chatwithai.network.model.WitAiResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import java.io.IOException
import kotlinx.coroutines.*

private const val INNER_TAG = "WitAiClient"
interface WitAiService {
    @Headers("Authorization: Bearer W2JD5CT3CDBYY2AVTMZQTYAMR5KTPWX2")
    @GET("message?v=20230406")
    fun getMessage(@Query("q") message: String): Call<WitAiResponse>
}

object WitAiClient {
    private var witAiService: WitAiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.wit.ai/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        witAiService = retrofit.create(WitAiService::class.java)
    }

    fun getInstance(): WitAiService {
        return witAiService
    }

    fun getDateTime(message: String, callback: (String?) -> Unit) {
        GlobalScope.launch(Dispatchers.Main) {
            val result = async(Dispatchers.IO) {
                val call = witAiService.getMessage(message)
                try {
                    val response = call.execute()
                    if (response.isSuccessful) {
                        Log.d(TAG, "[$INNER_TAG]:GetDateTimeTask success")
                        val witAiResponse = response.body()
                        if (witAiResponse != null) {
                            Log.d(TAG, "[$INNER_TAG]:GetDateTimeTask $witAiResponse")
                            val entities = witAiResponse.entities
                            if (entities.containsKey("wit\$datetime:datetime")) {
                                Log.d(TAG, "[$INNER_TAG]:GetDateTimeTask is contain date and time")
                                val datetime = entities["wit\$datetime:datetime"]?.get(0)
                                if (datetime != null) {
                                    Log.d(TAG, "[$INNER_TAG]:GetDateTimeTask datetime?.from?.value: ${datetime.from?.value}")
                                    return@async datetime.from?.value
                                }
                            }
                        }
                    }
                } catch (e: IOException) {
                    // Handle exception here
                }
                return@async null
            }
            callback(result.await())
        }
    }
}
