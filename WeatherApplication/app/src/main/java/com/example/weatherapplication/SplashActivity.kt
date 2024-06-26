package com.example.weatherapplication

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale

class SplashActivity : AppCompatActivity() {
    private var posNx: Double = 0.0 // 사용자의 위도
    private var posNy: Double = 0.0 // 사용자의 경도
    private var locationText = "" // 주소 정보를 저장
    private var subLocality = "" // 세부 지역 정보를 저장
    private val delayMillis: Long = 3000 // 스플래시 화면 표시

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        CoroutineScope(Dispatchers.Main).launch {
            getLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location: Location? ->
                location?.let {
                    posNx = it.latitude
                    posNy = it.longitude
                    val address = getAddress(it.latitude, it.longitude)?.get(0)
                    locationText = address?.let {
                        listOfNotNull(it.adminArea, it.locality, it.subLocality, it.thoroughfare)
                            .joinToString(" ")
                    }.toString()
                    subLocality = address?.subLocality.toString()
                    proceedToMainActivity()
                } ?: run {
                    showToast("Location not found")
                    proceedToMainActivity()
                }
            }
    }


    private fun getAddress(lat: Double, lng: Double): List<Address>? {
        lateinit var address: List<Address>

        return try {
            val geocoder = Geocoder(this, Locale.KOREA)
            address = geocoder.getFromLocation(lat, lng, 1) as List<Address>
            address.forEach { addr ->
                Log.d("MainActivity", "Address: ${addr.toString()}")
                Log.d("MainActivity", "Feature Name: ${addr.featureName}")
                Log.d("MainActivity", "Locality: ${addr.locality}")
                Log.d("MainActivity", "Admin Area: ${addr.adminArea}")
                Log.d("MainActivity", "Country Name: ${addr.countryName}")
                Log.d("MainActivity", "Postal Code: ${addr.postalCode}")
                Log.d("MainActivity", "Latitude: ${addr.latitude}")
                Log.d("MainActivity", "Longitude: ${addr.longitude}")
                Log.d("MainActivity", "subThought: ${addr.subThoroughfare}")
                Log.d("MainActivity", "subLatitude: ${addr.subLocality}")
            }
            address
        } catch (e: IOException) {
            Toast.makeText(this, "주소를 가져 올 수 없습니다", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun proceedToMainActivity() {
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this@SplashActivity, MainActivity::class.java).apply {
                putExtra("latitude", posNx)
                putExtra("longitude", posNy)
                putExtra("subLocality", subLocality)
                putExtra("locationText", locationText)
            }
            startActivity(intent)
            finish()
        }, delayMillis)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
