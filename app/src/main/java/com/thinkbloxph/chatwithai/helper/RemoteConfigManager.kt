package com.thinkbloxph.chatwithai.helper

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.thinkbloxph.chatwithai.R

object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"

    private const val CACHE_EXPIRATION_SECONDS = 3600L

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(CACHE_EXPIRATION_SECONDS)
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    fun load(onComplete: ((Boolean) -> Unit)? = null) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Remote config values successfully fetched and activated.")
                } else {
                    Log.w(TAG, "Error fetching remote config values: ${task.exception}")
                }
                onComplete?.invoke(task.isSuccessful)
            }
    }


    fun getString(key: String): String {
        return remoteConfig.getString(key)
    }

    fun getLong(key: String): Long {
        return remoteConfig.getLong(key)
    }

    fun getBoolean(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }
}