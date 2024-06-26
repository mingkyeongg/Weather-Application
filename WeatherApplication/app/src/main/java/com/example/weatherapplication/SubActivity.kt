package com.example.weatherapplication

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

data class DalleRequest(val prompt: String, val n: Int = 1, val size: String = "256x256")
data class DalleResponse(val data: List<ImageData>)
data class ImageData(val url: String)

interface OpenAIDalleApi {
    @Headers(
        "Content-Type: application/json",
        "Authorization: Bearer <인증키>"
    )
    @POST("v1/images/generations")
    fun generateImage(@Body request: DalleRequest): Call<DalleResponse>
}

object DalleApiClient {
    private var retrofit: Retrofit? = null

    fun getClient(): Retrofit {
        if (retrofit == null) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            val httpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl("https://api.openai.com/")
                .client(httpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!
    }
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    } else {
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
}

class SubActivity : AppCompatActivity() {
    private lateinit var backButton: Button
    private lateinit var weatherSummary: TextView
    private lateinit var recommendationTextView: TextView
    private lateinit var generatedImageView: ImageView
    private lateinit var imageExplain: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub)

        weatherSummary = findViewById(R.id.weather_summary)
        backButton = findViewById(R.id.back_button)
        recommendationTextView = findViewById(R.id.recommendation_text_view)
        generatedImageView = findViewById(R.id.generated_image_view)
        imageExplain = findViewById(R.id.image_explain)


        val temperature = intent.getStringExtra("TEMPERATURE")
        val location = intent.getStringExtra("LOCATION")
        val precipitation = intent.getStringExtra("RAINPROBABILITY")
        val dust = intent.getStringExtra("DUST")
        val maxTemperature = intent.getStringExtra("MAXTEMPERATURE")
        val minTemperature = intent.getStringExtra("MINTEMPERATURE")

        weatherSummary.text = "오늘 ${location}의\n 최고 온도는 $maxTemperature° 최저온도는 $minTemperature° 입니다."


        if (isNetworkAvailable(this)) {
            val promptImage = "Draw a cute character dressed appropriately for the current weather in $location. The temperature is $temperature degrees Celsius with a $precipitation% chance of rain. The maximum temperature is $maxTemperature degrees Celsius and the minimum temperature is $minTemperature degrees Celsius. The air quality is affected by dust levels at $dust."
            generateImage(promptImage)
            val promptRecommendation = "현재 ${location}의 날씨는 ${temperature}도이며, 최고 온도는 ${maxTemperature} 최저 온도는 ${minTemperature} 입니다. 강수확률은 ${precipitation}%이며 미세먼지의 농도가 ${dust} 입니다. 이러한 조건에 맞는 옷차림을 50글자 이내로 추천해주세요."
            generateRecommendation(promptRecommendation)
        } else {
            recommendationTextView.text = "No internet connection. Please check your connection and try again."
        }

        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
    private fun generateRecommendation(prompt: String) {
        val apiService = OpenAIClient.getClient().create(OpenAIGPTApi::class.java)
        val request = ChatGPTRequest(
            messages = listOf(ChatMessage(role = "user", content = prompt))
        )
        val call = apiService.generateText(request)
        call.enqueue(object : Callback<ChatGPTResponse> {
            override fun onResponse(call: Call<ChatGPTResponse>, response: Response<ChatGPTResponse>) {
                if (response.isSuccessful) {
                    val chatResponse = response.body()
                    val recommendation = chatResponse?.choices?.firstOrNull()?.message?.content ?: "추천을 생성할 수 없습니다."
                    recommendationTextView.text = recommendation

                    val imagePrompt = "Create an image of someone wearing the following outfit: $recommendation"
                    generateImage(imagePrompt)
                } else {
                    Log.e("API Error", "Response Error: ${response.message()}")
                    recommendationTextView.text = "Text Response Error: ${response.message()}"
                }
            }

            override fun onFailure(call: Call<ChatGPTResponse>, t: Throwable) {
                Log.e("API Error", "Failure Error: ${t.message}")
                recommendationTextView.text = "Text Fail Error: ${t.message}"
            }
        })
    }

    private fun generateImage(prompt: String) {
        val apiService = DalleApiClient.getClient().create(OpenAIDalleApi::class.java)
        val request = DalleRequest(prompt)

        val call = apiService.generateImage(request)
        call.enqueue(object : Callback<DalleResponse> {
            override fun onResponse(call: Call<DalleResponse>, response: Response<DalleResponse>) {
                if (response.isSuccessful) {
                    val dalleResponse = response.body()
                    if (dalleResponse != null && dalleResponse.data.isNotEmpty()) {
                        val imageUrl = dalleResponse.data[0].url
                       Log.d("API Success", "Image URL: $imageUrl")
                       Glide.with(this@SubActivity)
                            .load(imageUrl)
                            .into(generatedImageView)
                    } else {
                        imageExplain.text = "No image generated."
                    }
                } else {
                   Log.e("API Error", "Response Error: ${response.message()}")
                   imageExplain.text = "Image Response Error: ${response.message()}"
               }
           }

            override fun onFailure(call: Call<DalleResponse>, t: Throwable) {
                Log.e("API Error", "Failure Error: ${t.message}")
                imageExplain.text = "Image Fail Error: ${t.message}"
            }
        })
    }
}
