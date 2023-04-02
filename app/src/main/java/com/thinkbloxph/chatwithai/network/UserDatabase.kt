package com.thinkbloxph.chatwithai.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.thinkbloxph.chatwithai.TAG
import com.thinkbloxph.chatwithai.network.model.User

private const val INNER_TAG = "UserDatabase"
class UserDatabase public constructor(databaseUrl: String) {
    companion object {
        private var instance: UserDatabase? = null

        fun getInstance(databaseUrl: String): UserDatabase {
            if (instance == null) {
                instance = UserDatabase(databaseUrl)
            }
            return instance as UserDatabase
        }
    }

    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance(databaseUrl)

    fun saveCurrentUserToDatabase(user: User, completion: (isSuccess:Boolean) -> Unit) {
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Save the user object to the database under their unique ID
        if (currentUser != null) {
            // Get a reference to the users node in the Realtime Database
            val databaseRef = firebaseDatabase.getReference("users")

            // Create a map to only include the required fields
            val userMap = mapOf(
                "uid" to user.uid,
                "displayName" to user.displayName,
                "email" to user.email,
                "phoneNumber" to user.phoneNumber,
                "googleId" to user.googleId,
                "facebookId" to user.facebookId,
                "credit" to user.credit,
                "isSubscribed" to user.isSubscribed,
                "timestamp" to ServerValue.TIMESTAMP
            )

            // Save the user object to the database with only the required fields
            databaseRef.child(currentUser.uid).setValue(userMap)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.v(TAG, "[${INNER_TAG}]: saveCurrentUserToDatabase user.uid: ${user.uid} user.displayName: ${user.displayName}")
                        completion(true) // call the completion block with true if the save is successful
                    } else {
                        completion(false) // call the completion block with false if the save fails
                    }
                }
        }
    }

    fun updateCredit(currentCredit: Int, updateAmount: Int, callback: (resultCredit: Int?, isSuccess: Boolean) -> Unit) {
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        if(currentUser == null) {
            callback(null, false)
            return
        }

        // Get a reference to the users node in the Realtime Database
        val databaseRef = firebaseDatabase.getReference("users/${currentUser.uid}")

        databaseRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                // Get the current credit balance
                //val credit = currentData.child("credit").getValue(Int::class.java) ?: 0

                // Calculate the new credit balance
                val newCredit = currentCredit + updateAmount

                // If the new credit balance would be negative, cancel the transaction
                if (newCredit < 0) {
                    return Transaction.abort()
                }

                // Update the credit balance in the Realtime Database
                currentData.child("credit").value = newCredit

                // Set the transaction result
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    // Transaction failed
                    Log.v(TAG, "[${INNER_TAG}]: updateCredit transaction failed currentUser.uid: ${currentUser.uid}")
                    callback(null, false)
                } else if (committed) {
                    // Transaction successful
                    val newCredit = currentData?.child("credit")?.getValue(Int::class.java) ?: 0
                    Log.v(TAG, "[${INNER_TAG}]: updateCredit transaction success currentUser.uid: ${currentUser.uid}, new credit: $newCredit")
                    callback(newCredit, true)
                } else {
                    // Transaction canceled
                    Log.v(TAG, "[${INNER_TAG}]: updateCredit transaction canceled currentUser.uid: ${currentUser.uid}")
                    callback(null, false)
                }
            }
        })
    }

    fun updateSubscription(newStatus:Boolean){
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        if(currentUser!=null){
            // Get a reference to the users node in the Realtime Database
            val databaseRef = firebaseDatabase.getReference("users")

            // Update the user's isSubscribed field
            val updates = mapOf("isSubscribed" to newStatus)
            databaseRef.child(currentUser.uid).updateChildren(updates)
                .addOnSuccessListener {
                    // Update successful
                    Log.v(TAG, "[${INNER_TAG}]: updateSubscription success currentUser.uid: ${currentUser.uid}")
                }
                .addOnFailureListener { e ->
                    // Update failed
                    Log.v(TAG, "[${INNER_TAG}]: updateSubscription failed currentUser.uid: ${currentUser.uid}")
                }
        }
    }

    fun updateFacebookId(newFacebookId: String) {
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        if(currentUser != null) {
            // Get a reference to the users node in the Realtime Database
            val databaseRef = firebaseDatabase.getReference("users")

            // Update the user's facebookId field
            val updates = mapOf("facebookId" to newFacebookId)
            databaseRef.child(currentUser.uid).updateChildren(updates)
                .addOnSuccessListener {
                    // Update successful
                    Log.v(TAG, "[${INNER_TAG}]: updateFacebookId success currentUser.uid: ${currentUser.uid}")
                }
                .addOnFailureListener { e ->
                    // Update failed
                    Log.v(TAG, "[${INNER_TAG}]: updateFacebookId failed currentUser.uid: ${currentUser.uid}")
                }
        }
    }


    fun checkIfDataExists(uid: String,callback: (exists:Boolean) -> Unit) {
        val usersRef = firebaseDatabase.getReference("users")

        var exists = false

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                exists = dataSnapshot.exists()
                Log.v(TAG, "[${INNER_TAG}]: checkIfDataExists exist? ${exists} uid: ${uid}")
                callback(exists)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors that occur during the query
                Log.v(TAG, "[${INNER_TAG}]: checkIfDataExists canceled uid: ${uid}")
                exists = false
                callback(exists)
            }
        }
        usersRef.child(uid).addListenerForSingleValueEvent(listener)
    }

    fun loadUserData(uid: String, callback: (User?) -> Unit) {
        val userRef = firebaseDatabase.getReference("users/$uid")

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Check if the snapshot has data
                if (snapshot.exists()) {
                    // Convert the snapshot data to a User object
                    val user = snapshot.getValue(User::class.java)
                    // Call the callback with the user object
                    Log.v(TAG, "[${INNER_TAG}]: loadUserData success user.uid: ${user?.uid}")
                    callback(user)
                } else {
                    // No data exists for the given UID, call the callback with null
                    Log.v(TAG, "[${INNER_TAG}]: loadUserData failed")
                    callback(null)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle the error
                Log.e(TAG, "Error reading user data for UID $uid", error.toException())
                // Call the callback with null
                Log.v(TAG, "[${INNER_TAG}]: loadUserData failed")
                callback(null)
            }
        })
    }


    fun deleteCurrentUser() {
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Get a reference to the users node in the Realtime Database
            val databaseRef = firebaseDatabase.getReference("users")

            // Remove the user's data from the database
            databaseRef.child(currentUser.uid).removeValue()
                .addOnSuccessListener {
                    // Deletion successful
                    Log.v(TAG, "[${INNER_TAG}]: deleteCurrentUser success")
                }
                .addOnFailureListener { e ->
                    // Deletion failed
                    Log.v(TAG, "[${INNER_TAG}]: deleteCurrentUser failed")
                }
        }
    }
}