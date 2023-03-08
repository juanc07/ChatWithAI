package com.thinkbloxph.chatwithai.network.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class UserViewModel : ViewModel() {
    private val _firebaseUserId = MutableLiveData<String>()
    val firebaseUserId: LiveData<String> = _firebaseUserId

    private val _displayName = MutableLiveData<String>()
    val displayName: LiveData<String> = _displayName

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String> = _phoneNumber

    private val _googleUserId = MutableLiveData<String>()
    val googleUserId: LiveData<String> = _googleUserId

    private val _facebookUserId = MutableLiveData<String>()
    val facebookUserId: LiveData<String> = _facebookUserId

    private val _providerId = MutableLiveData<String>()
    val providerId: LiveData<String> = _providerId

    private val _credit = MutableLiveData<Int>()
    val credit: LiveData<Int> = _credit

    private val _isSubscribed = MutableLiveData<Boolean>()
    val isSubscribed: LiveData<Boolean> = _isSubscribed

    private val _createdDate = MutableLiveData<Any>()
    val createdDate: LiveData<Any> = _createdDate

    fun setFirebaseUserId(firebaseUserId: String) {
        _firebaseUserId.value = firebaseUserId
    }

    fun getFirebaseUserId(): String {
        return _firebaseUserId.value.toString()
    }

    fun setDisplayName(firebaseUserId: String) {
        _displayName.value = firebaseUserId
    }

    fun getDisplayName(): String {
        return _displayName.value.toString()
    }

    fun setProviderId(firebaseUserId: String) {
        _providerId.value = firebaseUserId
    }

    fun getProviderId(): String {
        return _providerId.value.toString()
    }

    fun setEmail(email: String) {
        _email.value = email
    }

    fun getEmail(): String {
        return _email.value.toString()
    }

    fun setPhoneNumber(phoneNumber: String) {
        _phoneNumber.value = phoneNumber
    }

    fun getPhoneNumber(): String {
        return _phoneNumber.value.toString()
    }

    fun getGoogleUserId(): String {
        return _googleUserId.value.toString()
    }

    fun setGoogleUserId(googleUserId: String) {
        _googleUserId.value = googleUserId
    }

    fun setFacebookUserId(facebookUserId: String) {
        _facebookUserId.value = facebookUserId
    }

    fun getFacebookUserId(): String {
        return _facebookUserId.value.toString()
    }

    fun setCredit(credit: Int) {
        _credit.value = credit
    }

    fun getCredit(): Int? {
        return _credit.value
    }

    fun setIsSubscribed(isSubscribed: Boolean) {
        _isSubscribed.value = isSubscribed
    }

    fun getIsSubscribed(): Boolean? {
        return _isSubscribed.value
    }

    fun setCreatedDate(createdDate: Any) {
        _createdDate.value = createdDate
    }

    fun getCreatedDate(): Any? {
        return _createdDate.value
    }

    fun reset() {
        _phoneNumber.value = ""
        _facebookUserId.value = ""
        _googleUserId.value = ""
        _firebaseUserId.value = ""
        _email.value = ""
    }
}