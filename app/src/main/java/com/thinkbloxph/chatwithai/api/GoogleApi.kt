package com.thinkbloxph.chatwithai.api

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thinkbloxph.chatwithai.R
import com.thinkbloxph.chatwithai.TAG

private const val INNER_TAG = "GoogleApi"
class GoogleApi(val _application: Application, val _activity: Activity, val _fragment: Fragment) {

    companion object {
        private lateinit var instance: GoogleApi

        fun initInstance(_application: Application, _activity: Activity, _fragment: Fragment) {
            instance = GoogleApi(_application, _activity, _fragment)
        }

        fun getInstance(): GoogleApi {
            return instance
        }
    }

    private lateinit var _context: Context
    lateinit var mGoogleSignInClient: GoogleSignInClient
    val Req_Code: Int = 123
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var signInIntent: Intent
    private lateinit var registerForActivityResult: ActivityResultLauncher<Intent>
    var successfulCallbackResult: (GoogleSignInAccount, FirebaseUser) -> Unit = { _, _->}
    var failedCallbackResult = {}

    fun init() {
        _context = _application?.getApplicationContext()!!

        FirebaseApp.initializeApp(_context)
        firebaseAuth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(_activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .requestProfile()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(_activity, gso)

        signInIntent = mGoogleSignInClient.signInIntent

        registerForActivityResult =
            _fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    if (task.isSuccessful) {
                        // The task completed successfully
                        val googlePhotoUrl =  task.getResult().photoUrl.toString()
                        val googlePhotoUrl2 = getGooglePhotoUrl(mGoogleSignInClient.signInIntent)

                        Log.v(TAG, "[${INNER_TAG}]: googlePhotoUrl: ${googlePhotoUrl}")
                        Log.v(TAG, "[${INNER_TAG}]: googlePhotoUrl2: ${googlePhotoUrl2}")

                        val account = task.result
                        val credential= GoogleAuthProvider.getCredential(account.idToken,null)

                        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {task->
                            if(task.isSuccessful) {
                                Log.v(TAG,"google sign in success")
                                val user = firebaseAuth.currentUser

                                if (user != null) {
                                    val facebookId = user.providerData.firstOrNull { it.providerId == "facebook.com" }?.uid
                                    if (facebookId != null) {
                                        Log.d("Facebook ID: ", facebookId)
                                    }
                                    val googleId = user.providerData.firstOrNull { it.providerId == "google.com" }?.uid
                                    if (googleId != null) {
                                        Log.d("Google ID: ", googleId)
                                    }

                                    val idToken =user?.getIdToken(false)
                                    // call login external here and pass user email and idToken

                                    SavedPreference.setEmail(_context,account.email.toString())
                                    SavedPreference.setUsername(_context,account.displayName.toString())

                                    Log.v(TAG,"${account.email}")
                                    Log.v(TAG,"${account.displayName}")

                                    successfulCallbackResult(account,user)
                                }else{
                                    Log.d(TAG, "[${INNER_TAG}]: google signIn failed user is null!")
                                    failedCallbackResult()
                                }
                            }else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "google signInWithCredential:failure", task.exception)
                            }
                        }

                        /*
                        // Extract the user's data from the account object
                        val email = account?.email
                        val displayName = account?.displayName
                        val idToken = account?.idToken

                        val givenName = account?.givenName
                        val familyName = account?.familyName
                        val personId = account?.id
                        val imageUrl = account?.photoUrl

                        Log.d(TAG, "[${INNER_TAG}]: check data pass in login screen!")
                        Log.d(TAG, "Check idToken: ${idToken}")
                        Log.d(TAG, "Check personId: ${personId}")
                        Log.d(TAG, "Check Email: ${email}")
                        Log.d(TAG, "Check displayName: ${displayName}")
                        Log.d(TAG, "Check givenName: ${givenName}")
                        Log.d(TAG, "Check familyName: ${familyName}")
                        Log.d(TAG, "Check imageUrl: ${imageUrl}")
                        */
                    } else if (task.isCanceled) {
                        // The task was canceled
                        Log.d(TAG, "[${INNER_TAG}]: google signIn canceled!")
                    } else {
                        // The task failed
                        val exception = task.exception
                        if (exception is ApiException) {
                            // An API exception occurred
                            val statusCode = exception.statusCode
                            Log.d(
                                TAG,
                                "[${INNER_TAG}]: google signIn Failed! statusCode: ${statusCode}"
                            )
                            failedCallbackResult()
                        } else {
                            // An unknown exception occurred
                            Log.d(
                                TAG,
                                "[${INNER_TAG}]: google signIn Failed! An unknown exception occurred!"
                            )
                        }
                    }
                }
            }
    }

    fun getUserInfo(user: FirebaseUser){
        Log.d(
            TAG,
            "[${INNER_TAG}]: getUserInfo Google user"
        )

        for (profile in user.providerData) {
            if (profile.providerId == "google.com") {
                val googleSignInClient = GoogleSignIn.getClient(_activity, GoogleSignInOptions.DEFAULT_SIGN_IN)
                val account = googleSignInClient.silentSignIn().result
                if (account != null) {
                    val name = account.displayName
                    val email = account.email
                    if (name != null) {
                        Log.d(
                            TAG,
                            "[${INNER_TAG}]: Google user name: $name"
                        )
                    }
                    if (email != null) {
                        Log.d(
                            TAG,
                            "[${INNER_TAG}]: Google user email: $email"
                        )
                    }
                }
            }
        }
    }

    fun getGooglePhotoUrl(data: Intent): Uri? {
        val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account: GoogleSignInAccount? =task.getResult(ApiException::class.java)
            if (account != null) {
                val photoUrl = task.getResult().photoUrl
                return photoUrl
            }
            return null
        } catch (e: ApiException){
            return null
        }
    }

    fun login(successCallback: (GoogleSignInAccount, FirebaseUser) -> Unit, failedCallback: () -> Unit) {
        successfulCallbackResult = successCallback
        failedCallbackResult = { failedCallback() }

        registerForActivityResult.launch(signInIntent)
    }

    fun signOut(signoutCallback: () -> Unit){
        mGoogleSignInClient.signOut().addOnCompleteListener {
            signoutCallback()
        }
    }
}