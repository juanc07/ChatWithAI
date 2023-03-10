package com.thinkbloxph.chatwithai.helper

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment

class PermissionManager private constructor() {

    private lateinit var permissionCallback: (Map<String, Boolean>) -> Unit
    private lateinit var permissionLauncher: (Array<String>) -> Unit

    fun checkPermissions(activity: Activity, permissions: Array<String>, callback: (Map<String, Boolean>) -> Unit) {
        permissionCallback = callback
        permissionLauncher = { ActivityCompat.requestPermissions(activity, permissions, REQUEST_CODE) }
        checkPermissions()
    }

    fun checkPermissions(fragment: Fragment, permissions: Array<String>, callback: (Map<String, Boolean>) -> Unit) {
        permissionCallback = callback
        permissionLauncher = { fragment.requestPermissions(permissions, REQUEST_CODE) }
        checkPermissions()
    }

    private fun checkPermissions() {
        val allPermissions = permissionLauncher
        val permissionResult = permissionCallback
        allPermissions?.let { permissionLauncher ->
            permissionResult?.let { permissionCallback ->
                permissionLauncher.invoke(PERMISSIONS)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE = 1
        private val PERMISSIONS = arrayOf(
            // add any permission you need to request here
            android.Manifest.permission.READ_PHONE_STATE,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )

        private var instance: PermissionManager? = null

        fun getInstance(): PermissionManager {
            return instance ?: synchronized(this) {
                instance ?: PermissionManager().also { instance = it }
            }
        }
    }
}

/*
how to use

val permissionManager = PermissionManager.getInstance()

// Call this function when you want to request permissions
permissionManager.checkPermissions(activity, arrayOf(
    Manifest.permission.READ_PHONE_STATE,
    Manifest.permission.READ_PHONE_NUMBERS,
    Manifest.permission.READ_SMS
)) { result ->
    // This code will be called when the user responds to the permission request
    val allAreGranted = result.all { it.value }
    if (allAreGranted) {
        // The user granted all the requested permissions
        getPhoneNumberByTelephonyManager()
    } else {
        // The user denied at least one of the requested permissions
        // Explain to the user why the permission is needed and offer to request again
        val builder = MaterialAlertDialogBuilder(_activity)
        builder.setMessage("This app requires the READ_PHONE_STATE permission to retrieve the phone number of the device. Without this permission, the app will not be able to function as expected.")
            .setPositiveButton("OK") { _, _ ->
                permissionManager.checkPermissions(activity, arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS,
                    Manifest.permission.READ_SMS
                )) { result2 ->
                    // This code will be called when the user responds to the permission request again
                    if (result2.all { it.value }) {
                        // The user granted all the requested permissions
                        getPhoneNumberByTelephonyManager()
                    } else {
                        // The user denied at least one of the requested permissions again
                        // Handle the error
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        val alert = builder.create()
        alert.show()
    }
}


 */
