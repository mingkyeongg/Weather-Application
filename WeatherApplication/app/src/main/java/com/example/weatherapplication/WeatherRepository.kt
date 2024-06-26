package com.example.weatherapplication

import okhttp3.Interceptor
import okhttp3.Response
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import com.google.gson.GsonBuilder
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import retrofit2.http.Query
import retrofit2.http.GET
import retrofit2.Response as RetrofitResponse
import java.io.IOException

// 데이터 클래스 정의
data class WeatherResponse(
    val response: ResponseData
)

data class ResponseData(
    val header: Header,
    val body: Body
)

data class Header(
    val resultCode: String,
    val resultMsg: String
)

data class Body(
    val dataType: String,
    val items: Items,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int
)

data class Items(
    val item: List<WeatherItem>
)

data class WeatherItem(
    val baseDate: String,
    val baseTime: String,
    val category: String,
    val fcstDate: String,
    val fcstTime: String,
    val fcstValue: String,
    val nx: Int,
    val ny: Int
)

interface WeatherApi {
    @GET("getVilageFcst")
    suspend fun getWeather(
        @Query("serviceKey") serviceKey: String,
        @Query("dataType") dataType: String,
        @Query("numOfRows") numOfRows: Int,
        @Query("pageNo") pageNo: Int,
        @Query("base_date") baseDate: String,
        @Query("base_time") baseTime: String,
        @Query("nx") nx: Int,
        @Query("ny") ny: Int
    ): RetrofitResponse<WeatherResponse>
}

// Retrofit 인스턴스 생성
object RetrofitInstance {
    private const val BASE_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    private val retryInterceptor = RetryInterceptor(maxRetries = 3)

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor(retryInterceptor)
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // 연결 타임아웃을 60초로 설정
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // 읽기 타임아웃을 60초로 설정
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS) // 쓰기 타임아웃을 60초로 설정
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(client)
        .build()

    fun createApi(): WeatherApi {
        return retrofit.create(WeatherApi::class.java)
    }
}

class RetryInterceptor(private val maxRetries: Int) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        var exception: IOException? = null
        var tryCount = 0

        while (tryCount < maxRetries && (response == null || !response.isSuccessful)) {
            try {
                response = chain.proceed(chain.request())
            } catch (e: IOException) {
                exception = e
            } finally {
                tryCount++
            }
        }

        // 마지막 시도에서 예외가 발생했으면 예외를 던짐
        if (response == null && exception != null) {
            throw exception
        }

        return response ?: throw IOException("Unknown error while attempting request retry.")
    }
}

class WeatherRepository {
    private val apiKey: String = "<인증키>" // 여기에 실제 API 키를 넣어주세요
    private val client = RetrofitInstance.createApi()

    fun getWeather(
        dataType: String,
        numOfRows: Int,
        pageNo: Int,
        baseDate: String,
        baseTime: String,
        nx: Int,
        ny: Int
    ): Flow<RetrofitResponse<List<WeatherItem>>> = flow {
        val response = client.getWeather(apiKey, dataType, numOfRows, pageNo, baseDate, baseTime, nx, ny)
        if (response.isSuccessful) {
            val filteredItems = response.body()?.response?.body?.items?.item?.filter {
                it.category in listOf("POP", "PTY", "TMP", "SKY", "TMN", "TMX")
            }
            emit(RetrofitResponse.success(filteredItems))
        } else {
            emit(RetrofitResponse.error(response.code(), response.errorBody()!!))
        }
    }
}

class WeatherViewModel(private val repository: WeatherRepository) : ViewModel() {
    fun getWeather(
        dataType: String,
        numOfRows: Int,
        pageNo: Int,
        baseDate: String,
        baseTime: String,
        nx: Int,
        ny: Int
    ) = repository.getWeather(dataType, numOfRows, pageNo, baseDate, baseTime, nx, ny).asLiveData()
}

class WeatherViewModelFactory(private val repository: WeatherRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WeatherViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WeatherViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
