package com.thinkbloxph.chatwithai.network

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.thinkbloxph.chatwithai.network.model.User

class UserDatabase public constructor() {
    companion object {
        private val instance = UserDatabase()

        fun getInstance(): UserDatabase {
            return instance
        }
    }

    // 1st save only when don't exists
    fun saveCurrentUserToDatabase(googleId:String) {
        // Get the current user from Firebase Authentication
        val currentUser = FirebaseAuth.getInstance().currentUser

        // Save the user object to the database under their unique ID
        if (currentUser != null) {
            // Get a reference to the users node in the Realtime Database
            val databaseRef = FirebaseDatabase.getInstance().getReference("users")

            // Create a new user object with the required fields
            val user = User(currentUser.uid, currentUser.displayName, currentUser.email, currentUser.phoneNumber, googleId, null, 5, false, System.currentTimeMillis())

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

    fun updateCredit(currentCredit:Int, deduction:Int):Boolean{
        var isSuccess = false
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
                    isSuccess =  true
                }
                .addOnFailureListener { e ->
                    // Update failed
                    isSuccess =   false
                }
        }

        return isSuccess
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
                }
                .addOnFailureListener { e ->
                    // Update failed
                }
        }
    }

    fun checkIfDataExists(uid: String): Boolean {
        val usersRef = FirebaseDatabase.getInstance().getReference("users")

        var exists = false

        val listener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                exists = dataSnapshot.exists()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors that occur during the query
                exists = false
            }
        }

        usersRef.child(uid).addListenerForSingleValueEvent(listener)

        return exists
    }
}