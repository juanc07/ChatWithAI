package com.thinkbloxph.chatwithai.helper

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private const val INNER_TAG = "FirebaseHelper"
class FirebaseHelper {
    fun getProvider(): String {
        return Firebase.auth.currentUser?.providerData?.lastOrNull()?.providerId ?: ""
    }

    fun getProviderId(provider: String): String? {
        return Firebase.auth.currentUser?.providerData?.find { it.providerId == provider }?.uid
    }

    fun getIdToken(successCallback: (phoneNumber: String, idToken: String, email: String) -> Unit, failedCallback: () -> Unit) {
        Firebase.auth.currentUser?.let { user ->
            user.getIdToken(true).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val idToken = task.result?.token
                    successCallback(user.phoneNumber ?: "", idToken ?: "", user.email ?: "")
                } else {
                    failedCallback()
                }
            }
        }
    }
}