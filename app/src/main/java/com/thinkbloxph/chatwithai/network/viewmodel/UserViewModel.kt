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

    private val _createdDate = MutableLiveData<Long>()
    val createdDate: LiveData<Long> = _createdDate

    private val _searchNumResults = MutableLiveData<Long>()
    val searchNumResults: LiveData<Long> = _searchNumResults

    private val _enableSearch = MutableLiveData<Boolean>()
    val enableSearch: LiveData<Boolean> = _enableSearch

    private val _creditUsage = MutableLiveData<Long>()
    val creditUsage: LiveData<Long> = _creditUsage

    private val _encryptedSearchApiKey = MutableLiveData<String>()
    val encryptedSearchApiKey: LiveData<String> = _encryptedSearchApiKey

    private val _searchApiSecretKey = MutableLiveData<String>()
    val searchApiSecretKey: LiveData<String> = _searchApiSecretKey

    private val _searchEngineId = MutableLiveData<String>()
    val searchEngineId: LiveData<String> = _searchEngineId

    private val _currentPrompt = MutableLiveData<String>()
    val currentPrompt: LiveData<String> = _currentPrompt

    private val _gptToken = MutableLiveData<String>()
    val gptToken: LiveData<String> = _gptToken

    private val _completionCreditPrice = MutableLiveData<Long>()
    val completionCreditPrice: LiveData<Long> = _completionCreditPrice

    private val _recordCreditPrice = MutableLiveData<Long>()
    val recordCreditPrice: LiveData<Long> = _recordCreditPrice

    private val _appVersion = MutableLiveData<String>()
    val appVersion: LiveData<String> = _appVersion

    private val _initialFreeCredit = MutableLiveData<Long>()
    val initialFreeCredit: LiveData<Long> = _initialFreeCredit

    private val _gptModel = MutableLiveData<String>()
    val gptModel: LiveData<String> = _gptModel

    private val _defaultDBUrl = MutableLiveData<String>()
    val defaultDBUrl: LiveData<String> = _defaultDBUrl

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

    fun setCredit(credit: Int?) {
        _credit.value = credit!!
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

    fun setCreatedDate(createdDate: Long) {
        _createdDate.value = createdDate
    }

    fun getCreatedDate(): Long? {
        return _createdDate.value
    }

    fun setSearchNumResults(value: Long) {
        _searchNumResults.value = value
    }

    fun getSearchNumResults(): Long? {
        return _searchNumResults.value
    }

    fun setEnableSearch(value: Boolean) {
        _enableSearch.value = value
    }

    fun getEnableSearch(): Boolean? {
        return _enableSearch.value
    }

    fun setCreditUsage(value: Long) {
        _creditUsage.value = value
    }

    fun getCreditUsage(): Long? {
        return _creditUsage.value
    }

    fun setEncryptedSearchApiKey(value: String) {
        _encryptedSearchApiKey.value = value
    }

    fun getEncryptedSearchApiKey(): String {
        return _encryptedSearchApiKey.value.toString()
    }

    fun setSearchApiSecretKey(value: String) {
        _searchApiSecretKey.value = value
    }

    fun getSearchApiSecretKey(): String {
        return _searchApiSecretKey.value.toString()
    }

    fun setSearchEngineId(value: String) {
        _searchEngineId.value = value
    }

    fun getSearchEngineId(): String {
        return _searchEngineId.value.toString()
    }

    fun setCurrentPrompt(value: String) {
        _currentPrompt.value = value
    }

    fun getCurrentPrompt(): String {
        return _currentPrompt.value.toString()
    }

    fun setGptToken(value: String) {
        _gptToken.value = value
    }

    fun getGptToken(): String {
        return _gptToken.value.toString()
    }

    fun setCompletionCreditPrice(value: Long) {
        _completionCreditPrice.value = value
    }

    fun getCompletionCreditPrice(): Long? {
        return _completionCreditPrice.value
    }

    fun setRecordCreditPrice(value: Long) {
        _recordCreditPrice.value = value
    }

    fun getRecordCreditPrice(): Long? {
        return _recordCreditPrice.value
    }

    fun setAppVersion(value: String) {
        _appVersion.value = value
    }

    fun getAppVersion(): String {
        return _appVersion.value.toString()
    }

    fun setInitialFreeCredit(value: Long) {
        _initialFreeCredit.value = value
    }

    fun getInitialFreeCredit(): Long? {
        return _initialFreeCredit.value
    }

    fun setGptModel(value: String) {
        _gptModel.value = value
    }

    fun getGptModel(): String {
        return _gptModel.value.toString()
    }

    fun setDefaultDBUrl(value: String) {
        _defaultDBUrl.value = value
    }

    fun getDefaultDBUrl(): String {
        return _defaultDBUrl.value.toString()
    }

    fun reset() {
        _phoneNumber.value = ""
        _facebookUserId.value = ""
        _googleUserId.value = ""
        _firebaseUserId.value = ""
        _email.value = ""
        _enableSearch.value = true
        _searchNumResults.value = 3
        _creditUsage.value = 1
        _completionCreditPrice.value = 1
        _recordCreditPrice.value = 5
    }
}