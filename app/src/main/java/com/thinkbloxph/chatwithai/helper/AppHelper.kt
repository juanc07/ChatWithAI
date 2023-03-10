package com.thinkbloxph.chatwithai.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import java.util.*

class AppHelper private constructor() {

    private lateinit var context: Context
    private lateinit var locationManager: LocationManager

    private var language: String = ""
    private var country: String = ""

    companion object {
        private var instance: AppHelper? = null

        @Synchronized
        fun getInstance(context: Context): AppHelper {
            if (instance == null) {
                instance = AppHelper()
                instance!!.init(context)
            }
            return instance!!
        }
    }

    private fun init(context: Context) {
        this.context = context
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        language = Locale.getDefault().language
        country = getCountryFromLocation()
    }

    fun getLocation(): Location? {
        var location: Location? = null
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }
        return location
    }

    fun getLanguage(): String {
        return language
    }

    fun getCountry(): String {
        return country
    }

    private fun getCountryFromLocation(): String {
        var country = ""
        val location = getLocation()
        if (location != null) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    country = addresses[0].countryCode
                }
            }
        }
        return country
    }

    /*
    how to use
     // Initialize the AppHelper singleton
        AppHelper.getInstance().init(applicationContext)

        // Use the AppHelper singleton to get the country
        val country = AppHelper.getInstance().getCountry()
        Log.d("MainActivity", "Country: $country")

     */
}
