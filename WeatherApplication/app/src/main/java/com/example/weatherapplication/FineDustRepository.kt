package com.example.weatherapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// 데이터 클래스 정의
data class FineDustResponse(
    val response: FineDustResponseData
)

data class FineDustResponseData(
    val header: FineDustHeader,
    val body: FineDustBody
)

data class FineDustHeader(
    val resultCode: String,
    val resultMsg: String
)

data class FineDustBody(
    val dataType: String,
    val items: List<FineDustItem>,
    val pageNo: Int,
    val numOfRows: Int,
    val totalCount: Int
)

data class FineDustItem(
    val dataTime: String,
    val stationName: String,
    val pm10Value: Int,
    val pm25Value: Int
)


// Retrofit 인터페이스 정의
interface FineDustApi {
    @GET("getMsrstnAcctoRltmMesureDnsty")
    suspend fun getFineDust(
        @Query("serviceKey") serviceKey: String,
        @Query("returnType") dataType: String,
        @Query("numOfRows") numOfRows: Int,
        @Query("pageNo") pageNo: Int,
        @Query("stationName") stationName: String,
        @Query("dataTerm") dataTerm: String,
        @Query("ver") ver: Float
    ): Response<FineDustResponse>
}

// Retrofit 인스턴스 생성
object FineDustRetrofitInstance {
    private const val BASE_URL = "http://apis.data.go.kr/B552584/ArpltnInforInqireSvc/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        setLevel(HttpLoggingInterceptor.Level.BODY)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val gson = GsonBuilder()
        .setLenient()
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(client)
        .build()

    fun createApi(): FineDustApi {
        return retrofit.create(FineDustApi::class.java)
    }
}

// FineDustRepository 정의
class FineDustRepository {
    private val apiKey: String = "<인증키>" // 여기에 실제 API 키를 넣어주세요
    private val client = FineDustRetrofitInstance.createApi()

    suspend fun getFineDustData(
        dataType: String,
        numOfRows: Int,
        pageNo: Int,
        stationName: String,
        dataTerm: String,
        ver: Float
    ): List<FineDustItem>? {
        val response = client.getFineDust(apiKey, dataType, numOfRows, pageNo, stationName, dataTerm, ver)
        return if (response.isSuccessful) {
            response.body()?.response?.body?.items
        } else {
            null
        }
    }
}


// FineDustViewModel 정의
class FineDustViewModel(private val repository: FineDustRepository) : ViewModel() {
    suspend fun getFineDustData(
        dataType: String,
        numOfRows: Int,
        pageNo: Int,
        stationName: String,
        dataTerm: String,
        ver: Float
    ): List<FineDustItem>? {
        return repository.getFineDustData(dataType, numOfRows, pageNo, stationName, dataTerm, ver)
    }
}

// FineDustViewModelFactory 정의
class FineDustViewModelFactory(private val repository: FineDustRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FineDustViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FineDustViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
