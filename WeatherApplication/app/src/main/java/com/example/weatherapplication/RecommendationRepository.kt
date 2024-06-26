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

data class ChatGPTRequest(
    val model: String = "gpt-3.5-turbo",
    val messages: List<ChatMessage>
)

data class ChatGPTResponse(
    val id: String,
    val choices: List<Choice>
)

data class Choice(
    val message: ChatMessage
)

data class ChatMessage(
    val role: String,
    val content: String
)

interface OpenAIGPTApi {
    @Headers(
        "Content-Type: application/json",
        "Authorization: Bearer <인증키>"
    )
    @POST("v1/chat/completions")
    fun generateText(@Body request: ChatGPTRequest): Call<ChatGPTResponse>
}



object OpenAIClient {
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
