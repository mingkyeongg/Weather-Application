package com.example.weatherapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.health.connect.datatypes.units.Temperature
import android.location.Address
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
import kotlinx.coroutines.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

    private lateinit var fineDustIcon: ImageView
    private lateinit var UltrafineDustIcon: ImageView
    private lateinit var currentTimeTextView: TextView
    private lateinit var currentDateTextView: TextView
    private lateinit var currentTemperature: TextView
    private lateinit var job: Job
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLocation: TextView
    private lateinit var fineDustViewModel: FineDustViewModel
    private lateinit var rainProbability: TextView
    private lateinit var weatherIcon: ImageView
    private var fineDust: Int = 0
    private var ultraFineDust: Int = 0
    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var highLowTemperature: TextView
    private lateinit var mainLayout: RelativeLayout
    private var maxTemperature: String = ""
    private var minTemperature: String = ""
    private lateinit var whatToWearBtn: Button
    private lateinit var finedustText: TextView
    private lateinit var ultraFinedustText: TextView


    override fun onStart() {
        super.onStart()
        RequestPermissionsUtil(this).requestLocation() // 위치 권한 요청
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val repository = WeatherRepository()
        val factory = WeatherViewModelFactory(repository)

        val fineDustRepository = FineDustRepository()
        val fineDustFactory = FineDustViewModelFactory(fineDustRepository)
        fineDustViewModel = ViewModelProvider(this, fineDustFactory).get(FineDustViewModel::class.java)

        val latitude = intent.getDoubleExtra("latitude", 0.0)
        val longitude = intent.getDoubleExtra("longitude", 0.0)
        val subLocality = intent.getStringExtra("subLocality")
        val locationText = intent.getStringExtra("locationText")


        fineDustIcon = findViewById(R.id.fine_dust_img)
        UltrafineDustIcon = findViewById(R.id.ultrafine_dust_img)
        currentDateTextView = findViewById(R.id.current_date)
        currentTimeTextView = findViewById(R.id.current_time)
        currentTemperature = findViewById(R.id.current_temperature)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        currentLocation = findViewById(R.id.current_location)
        rainProbability = findViewById(R.id.rain_probability)
        weatherIcon = findViewById(R.id.weather_icon)
        highLowTemperature = findViewById(R.id.high_low_temperature)
        mainLayout = findViewById(R.id.main_layout)
        whatToWearBtn = findViewById(R.id.what_to_wear_button)
        finedustText = findViewById(R.id.fine_dust_text)
        ultraFinedustText = findViewById(R.id.ultrafine_dust_text)


        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        val (currentDate, baseTime) = getAdjustedDateTime()
        val (highLowDate, highLowBaseTime) = getBaseDateTime()


        currentLocation.text = locationText

        val weatherRepository = WeatherRepository()
        val weatherFactory = WeatherViewModelFactory(weatherRepository)
        weatherViewModel = ViewModelProvider(this, weatherFactory).get(WeatherViewModel::class.java)

        // 코루틴이 메인 스레드(UI)에서 실행되도록
        CoroutineScope(Dispatchers.Main).launch {
            weatherViewModel.getWeather(
                dataType = "JSON",
                numOfRows = 846,
                pageNo = 1,
                baseDate = currentDate,
                baseTime = baseTime,
                nx = latitude.toInt(),
                ny = longitude.toInt()
            ).observe(this@MainActivity) { response ->
                if (response.isSuccessful) {
                    val weatherData = response.body()
                    val popData = mutableListOf<WeatherItem>()
                    val ptyData = mutableListOf<WeatherItem>()
                    val tmpData = mutableListOf<WeatherItem>()
                    val skyData = mutableListOf<WeatherItem>()

                    weatherData?.forEach { item ->
                        when (item.category) {
                            "POP" -> popData.add(item)
                            "PTY" -> ptyData.add(item)
                            "TMP" -> tmpData.add(item)
                            "SKY" -> skyData.add(item)
                        }

                        val tmpValues = tmpData.map { it.fcstValue.toIntOrNull() ?: 0 }
                        val skyValues = skyData.map { it.fcstValue.toIntOrNull() ?: 0 }
                        val ptyValues = ptyData.map { it.fcstValue.toIntOrNull() ?: 0 }
                        val popValues = popData.map { it.fcstValue.toIntOrNull() ?: 0 }

                        if (tmpValues.isNotEmpty()) {
                            currentTemperature.text = tmpValues[0].toString() + "°"
                        }
                        if (skyValues.isNotEmpty() && ptyValues.isNotEmpty()) {
                            updateWeatherIcon(skyValues[0], ptyValues[0])
                        }
                        if (popValues.isNotEmpty()) {
                            rainProbability.text = popValues[0].toString() + "%"
                        }
                    }

                    Log.d("WeatherData", "POP Data: $popData")
                    Log.d("WeatherData", "PTY Data: $ptyData")
                    Log.d("WeatherData", "TMP Data: $tmpData")
                    Log.d("WeatherData", "SKY Data: $skyData")
                } else {
                    Log.e("WeatherData", "Error: ${response.errorBody()?.string()}")
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            weatherViewModel.getWeather(
                dataType = "JSON",
                numOfRows = 846,
                pageNo = 1,
                baseDate = highLowDate,
                baseTime = highLowBaseTime,
                nx = latitude.toInt(),
                ny = longitude.toInt()
            ).observe(this@MainActivity) { response ->
                if (response.isSuccessful) {
                    val weatherData = response.body()
                    val tmnData = mutableListOf<WeatherItem>()
                    val tmxData = mutableListOf<WeatherItem>()

                    weatherData?.forEach { item ->
                        when (item.category) {
                            "TMN" -> tmnData.add(item)
                            "TMX" -> tmxData.add(item)
                        }

                        val tmnValues = tmnData.map { it.fcstValue.toFloatOrNull() ?: 0 }
                        val tmxValues = tmxData.map { it.fcstValue.toFloatOrNull() ?: 0 }

                        if (tmnValues.isNotEmpty() && tmxValues.isNotEmpty()) {
                            val maxTemp = tmxValues[0].toInt().toString()
                            val minTemp = tmnValues[0].toInt().toString()
                            highLowTemperature.text = "최고: $maxTemp° 최저: $minTemp°"
                            maxTemperature = maxTemp
                            minTemperature = minTemp
                        }
                    }

                    Log.d("WeatherData", "TMN Data: $tmnData")
                    Log.d("WeatherData", "TMX Data: $tmxData")
                } else {
                    Log.e("WeatherData", "Error: ${response.errorBody()?.string()}")
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            val fineDustData = fineDustViewModel.getFineDustData(
                dataType = "json",
                numOfRows = 10,
                pageNo = 1,
                stationName = "종로구", // 이거 API 또 따와야함 고민해볼것
                dataTerm = "DAILY",
                ver = 1.3f
            )
            fineDustData?.forEach{item ->
                fineDust = item.pm10Value
                ultraFineDust = item.pm25Value
            }
            Log.d("FineDustData", "$subLocality")
            Log.d("FineDustData", "$fineDust")
            Log.d("FineDustData", "$ultraFineDust")
            updateFinedustIcon(fineDust)
            updateUltraFinedustIcon(ultraFineDust)
        }

        job = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                updateCurrentTime()
                delay(60000) // 1초마다 업데이트
            }
        }

        val whatToWearButton: Button = findViewById(R.id.what_to_wear_button)
        whatToWearButton.setOnClickListener {
            val intent = Intent(this, SubActivity::class.java).apply {
                putExtra("TEMPERATURE", currentTemperature.text.toString())
                putExtra("LOCATION", currentLocation.text.toString())
                putExtra("DUST", fineDust.toString())
                putExtra("MAXTEMPERATURE", maxTemperature)
                putExtra("MINTEMPERATURE", minTemperature)
                putExtra("RAINPROBABLITY", rainProbability.text.toString())
            }
            startActivity(intent)
        }
    }

    private fun isDayTime(): Boolean {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        return hour in 7..18 // 오전 7시부터 오후 6시까지를 낮으로 간주
    }

    private fun updateWeatherIcon(SKY: Int, PTY: Int) {
//        - 하늘상태(SKY) 코드 : 맑음(1), 구름많음(3), 흐림(4)
//        - 강수형태(PTY) 코드 : (단기) 없음(0), 비(1), 비/눈(2), 눈(3), 소나기(4)
        if (SKY == 3 && PTY == 0) {
            weatherIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.weather_sunny_cloud))
            if (isDayTime()) {
                mainLayout.setBackgroundResource(R.drawable.day_cloudy)
            } else {
                mainLayout.setBackgroundResource(R.drawable.night_cloudy)
            }
        } else if (SKY == 4 && PTY == 0) {
            weatherIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.weather_cloud))
            if (isDayTime()) {
                changeTextColor(R.color.day_text_color)
                mainLayout.setBackgroundResource(R.drawable.day_cloudy)
            } else {
                mainLayout.setBackgroundResource(R.drawable.night_cloudy)
            }
        } else if (PTY == 1 || PTY == 4) {
            weatherIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.weather_rainy))
            if (isDayTime()) {
                mainLayout.setBackgroundResource(R.drawable.day_rainy)
            } else {
                mainLayout.setBackgroundResource(R.drawable.night_rainy)
            }
        } else if (PTY == 2) {
            weatherIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.weather_rainy_snowy))
            if (isDayTime()) {
                mainLayout.setBackgroundResource(R.drawable.day_rainy)
            } else {
                mainLayout.setBackgroundResource(R.drawable.night_rainy)
            }
        } else if (PTY == 3) {
            weatherIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.weather_snow))
            if (isDayTime()) {
                mainLayout.setBackgroundResource(R.drawable.day_snowy)
                changeTextColor(R.color.day_text_color)
            } else {
                mainLayout.setBackgroundResource(R.drawable.night_snowy)
            }
        } else {
//            if (isDayTime()) {
//                mainLayout.setBackgroundResource(R.drawable.day_sunny)
//            } else {
                mainLayout.setBackgroundResource(R.drawable.night_sunny)
//            }
        }
    }

    private fun changeTextColor(colorResId: Int) {
        val color = ContextCompat.getColor(this, colorResId)
        currentTimeTextView.setTextColor(color)
        currentDateTextView.setTextColor(color)
        currentTemperature.setTextColor(color)
        currentLocation.setTextColor(color)
        rainProbability.setTextColor(color)
        highLowTemperature.setTextColor(color)
        finedustText.setTextColor(color)
        ultraFinedustText.setTextColor(color)
        whatToWearBtn.setTextColor(color)
    }



    private fun updateFinedustIcon(value: Int) {
        when {
            value <= 30 -> {
                fineDustIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_status_low))
                fineDustIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_low))
            }
            value <= 80 -> {
                fineDustIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_status_medium))
                fineDustIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_medium))
            }
            value <= 150 -> {
                fineDustIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_status_high))
                fineDustIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_high))
            }
            else -> {
                fineDustIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_status_critical))
                fineDustIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_critical))
            }
        }
    }

    private fun updateUltraFinedustIcon(value: Int) {
        when {
            value <= 30 -> {
                UltrafineDustIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_status_low))
                UltrafineDustIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_low))
            }
            value <= 80 -> {
                UltrafineDustIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_status_medium))
                UltrafineDustIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_medium))
            }
            value <= 150 -> {
                UltrafineDustIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_status_high))
                UltrafineDustIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_high))
            }
            else -> {
                UltrafineDustIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_status_critical))
                UltrafineDustIcon.setColorFilter(ContextCompat.getColor(this, R.color.color_critical))
            }
        }
    }

    private fun updateCurrentTime() {
        val calendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MM/dd(EEE)", Locale.getDefault())
        val currentTime = timeFormat.format(calendar.time)
        val currentDate = dateFormat.format(calendar.time)
        currentTimeTextView.text = currentTime
        currentDateTextView.text = currentDate
    }


    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    fun getBaseDateTime(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
        val timeFormat = SimpleDateFormat("HHmm", Locale.KOREA)

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        return if (currentHour < 2 || (currentHour == 2 && currentMinute < 10)) {
            calendar.add(Calendar.DATE, -1)
            val previousDay = dateFormat.format(calendar.time)
            val baseTime = "0200"
            Pair(previousDay, baseTime)
        } else {
            val currentDate = dateFormat.format(calendar.time)
            val baseTime = "0200"
            Pair(currentDate, baseTime)
        }
    }



    fun getAdjustedDateTime(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.KOREA)
        val timeFormat = SimpleDateFormat("HHmm", Locale.KOREA)

        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        if (currentHour < 2 || (currentHour == 2 && currentMinute < 10)) {
            calendar.add(Calendar.DATE, -1)
            val previousDay = dateFormat.format(calendar.time)
            val previousTime = "2300"
            return Pair(previousDay, previousTime)
        } else {
            val currentDate = dateFormat.format(calendar.time)
            val baseTime = getCurrentBaseTime()
            return Pair(currentDate, baseTime)
        }
    }

    fun getCurrentBaseTime(): String {
        val baseTimes = listOf("0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300")
        val currentTime = Calendar.getInstance()

        var closestTime = baseTimes[0]
        var minDifference = Int.MAX_VALUE

        for (baseTime in baseTimes) {
            val baseTimeCalendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, baseTime.substring(0, 2).toInt())
                set(Calendar.MINUTE, baseTime.substring(2, 4).toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (baseTimeCalendar.after(currentTime)) {
                baseTimeCalendar.add(Calendar.DAY_OF_MONTH, -1)
            }

            val difference = Math.abs(currentTime.timeInMillis - baseTimeCalendar.timeInMillis).toInt()
            if (difference < minDifference) {
                minDifference = difference
                closestTime = baseTime
            }
        }

        return closestTime
    }
}
