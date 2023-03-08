package com.thinkbloxph.chatwithai.api

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import com.facebook.*
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thinkbloxph.chatwithai.TAG
import org.json.JSONException

private const val INNER_TAG = "FacebookApi"
class FacebookApi(val _application: Application, val _activity: Activity, val _fragment: Fragment) {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var callbackManager: CallbackManager

    private lateinit var _context: Context

    var successfulCallbackResult: (FirebaseUser?) -> Unit = {}
    var failedCallbackResult = {}

    lateinit var email:String
    lateinit var phoneNumber:String

    fun init() {
        _context = _application?.getApplicationContext()!!

        FacebookSdk.sdkInitialize(_application.getApplicationContext())
        AppEventsLogger.activateApp(_application)

        firebaseAuth = Firebase.auth

        callbackManager = CallbackManager.Factory.create()


        LoginManager.getInstance()
            .registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
                override fun onCancel() {
                    Log.d(TAG, "[${INNER_TAG}]: facebook:onCancel registerCallback")
                }

                override fun onError(error: FacebookException) {
                    Log.d(TAG, "[${INNER_TAG}]: facebook:onError registerCallback")
                }

                override fun onSuccess(loginResult: LoginResult) {
                    Log.d(TAG, "[${INNER_TAG}]: facebook:onSuccess:$loginResult")
                    handleFacebookAccessToken(loginResult.accessToken)
                }
            })
    }

    fun login(successCallback: (FirebaseUser?) -> Unit, failedCallback: () -> Unit) {
        successfulCallbackResult = successCallback
        failedCallbackResult = { failedCallback() }

        LoginManager.getInstance()
            .logInWithReadPermissions(_fragment, callbackManager, listOf("email", "public_profile"))
        //.logInWithReadPermissions(_fragment, callbackManager, listOf("email", "user_birthday"))
        //.logInWithReadPermissions(_fragment, callbackManager, listOf("email", "user_gender"))
        //.logInWithReadPermissions(_fragment, callbackManager, listOf("email", "public_profile","user_mobile_phone","user_phone_number"))

    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        Log.d(TAG, "[${INNER_TAG}]: handleFacebookAccessToken:$token")

        val credential = FacebookAuthProvider.getCredential(token.token)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(_activity) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "[${INNER_TAG}]: signInWithCredential:success")
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        val idToken =user?.getIdToken(false)
                        // call login external here and pass user email and idToken
                        updateUI()
                        successfulCallbackResult(user)
                    } else {
                        Log.w(TAG, "[${INNER_TAG}]: login successful but user data is null!")
                        failedCallbackResult();
                    }
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "[${INNER_TAG}]: signInWithCredential:failure", task.exception)
                    failedCallbackResult();
                }
            }
    }

    // for checking provider data
    fun checkProviderData(user: FirebaseUser){
        for (profile in user.providerData) {
            if (profile.providerId == "facebook.com") {
                val facebookId = profile.uid
                Log.d("Facebook ID: ", facebookId)
            } else if (profile.providerId == "google.com") {
                val googleId = profile.uid
                Log.d("Google ID: ", googleId)
            }
        }
    }

    private fun updateUI() {
        Log.d(TAG, "[${INNER_TAG}]: start fb GraphRequest")
        val request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken()) { `object`, _ ->
            Log.d(TAG, `object`.toString())
            try {
                val firstName = `object`?.getString("first_name")
                val lastName = `object`?.getString("last_name")
                email = `object`?.getString("email").toString()
                phoneNumber = `object`?.getString("phone").toString()
                val birthday = `object`?.getString("birthday")
                val gender = `object`?.getString("gender")
                val id = `object`?.getString("id")
                val photoUrl = "https://graph.facebook.com/$id/picture?type=normal"
                val fullName = "$firstName $lastName"
                val profileObjectImage =`object`?.getJSONObject("picture")?.getJSONObject("data")?.get("url").toString()


                Log.d(TAG, "[${INNER_TAG}]: check additional fb info")
                Log.d(TAG, "firstName: ${firstName}")
                Log.d(TAG, "lastName: ${lastName}")
                Log.d(TAG, "email: ${email}")
                Log.d(TAG, "phoneNumber: ${phoneNumber}")
                Log.d(TAG, "birthday: ${birthday}")
                Log.d(TAG, "gender: ${gender}")
                Log.d(TAG, "id: ${id}")
                Log.d(TAG, "photoUrl: ${photoUrl}")
                Log.d(TAG, "fullName: ${fullName}")
                Log.d(TAG, "profileObjectImage: ${profileObjectImage}")


            } catch (e: JSONException) {
                e.printStackTrace()
                Log.e(TAG, "signInResult: $e")
            }
        }
        val parameters = Bundle()
        parameters.putString("fields", "user_gender,first_name,last_name,email,id,link,picture.type(large)")
        //parameters.putString("fields", "first_name,last_name,email,phone,id,link,picture.type(large)")
        //parameters.putString("fields", "first_name,last_name,email,birthday,id,link,picture.type(large)")
        //parameters.putString("fields", "first_name,last_name,email,gender,id,link,picture.type(large)")
        request.parameters = parameters
        request.executeAsync()
    }

    fun signOut(){

    }
}