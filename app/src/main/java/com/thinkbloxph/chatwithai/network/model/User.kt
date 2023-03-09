package com.thinkbloxph.chatwithai.network.model

import com.google.firebase.database.ServerValue
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class User (
    var uid:String? = null,
    var displayName:String? = null,
    var email: String? = null,
    var phoneNumber: String? = null,
    var googleId: String? = null,
    var facebookId: String? = null,
    var credit: Int? = null,
    @field:JvmField
    var isSubscribed: Boolean? = null,
    val timestamp: Long = 0
){
    constructor() : this(null, null, null,null,null,null,5,false)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "uid" to uid,
            "displayName" to displayName,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "googleId" to googleId,
            "facebookId" to facebookId,
            "credit" to credit,
            "isSubscribed" to isSubscribed
        )
    }
}