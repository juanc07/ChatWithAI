package com.thinkbloxph.chatwithai.network

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.thinkbloxph.chatwithai.TAG
import com.thinkbloxph.chatwithai.network.model.User

private const val INNER_TAG = "UserDatabase"
class UserDatabase public constructor() {
    companion object {
        private val instance = UserDatabase()

        fun getInstance(): UserDatabase {
            return instance
        }
    }

    fun saveCurrentUserToDatabase(user: User, completion: (isSuccess:Boolean) -> Unit) {
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Save the user object to the database under their unique ID
        if (currentUser != null) {
            // Get a reference to the users node in the Realtime Database
            val databaseRef = FirebaseDatabase.getInstance().getReference("users")

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
                "timestamp" to user.createdDate
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



    fun updateCurrentUserToDatabase(googleId:String,facebookId:String, credit:Int,isSubscribed:Boolean) {
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser != null) {
            // Get a reference to the users node in the Realtime Database
            val databaseRef = FirebaseDatabase.getInstance().getReference("users")

            // Create a new user object
            val updatedUser = User(currentUser?.uid, currentUser?.displayName, currentUser?.email,currentUser?.phoneNumber,googleId,facebookId,credit,isSubscribed,System.currentTimeMillis())

            // Update the user object at their existing location in the database
            databaseRef.child(currentUser.uid).updateChildren(updatedUser.toMap())
        }
    }

    fun updateCredit(currentCredit:Int, deduction:Int,callback: (resultCredit: Int,isSuccess:Boolean) -> Unit){
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Get a reference to the users node in the Realtime Database
        val databaseRef = FirebaseDatabase.getInstance().getReference("users")

        // Deduct credits from the user's balance
        val newCredit = maxOf(currentCredit - deduction, 0)
        val updates = mapOf("credit" to newCredit)

        if(currentUser!=null){
            databaseRef.child(currentUser.uid).updateChildren(updates)
                .addOnSuccessListener {
                    // Update successful
                    Log.v(TAG, "[${INNER_TAG}]: updateCredit success currentUser.uid: ${currentUser.uid}")
                    callback(newCredit,true)
                }
                .addOnFailureListener { e ->
                    // Update failed
                    Log.v(TAG, "[${INNER_TAG}]: updateCredit failed currentUser.uid: ${currentUser.uid}")
                    callback(newCredit,false)
                }
        }
    }

    fun updateSubscription(newStatus:Boolean){
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        if(currentUser!=null){
            // Get a reference to the users node in the Realtime Database
            val databaseRef = FirebaseDatabase.getInstance().getReference("users")

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
            val databaseRef = FirebaseDatabase.getInstance().getReference("users")

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
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

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
        val userRef = FirebaseDatabase.getInstance().getReference("users/$uid")
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
            val databaseRef = FirebaseDatabase.getInstance().getReference("users")

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