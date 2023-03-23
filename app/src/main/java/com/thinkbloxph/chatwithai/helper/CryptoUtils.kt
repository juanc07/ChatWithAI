package com.thinkbloxph.chatwithai.helper

import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.security.spec.KeySpec
import java.util.Base64

object CryptoUtils {
    private const val salt = "my_salt"
    private const val iterations = 10000
    private const val keyLength = 256

    private fun generateKey(password: String): SecretKeySpec {
        val saltBytes = salt.toByteArray(Charsets.UTF_8)
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), saltBytes, iterations, keyLength)
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val secretKeyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(secretKeyBytes, "AES")
    }

    fun encrypt(apiKey: String, password: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, generateKey(password))
        return Base64.getEncoder().encodeToString(cipher.doFinal(apiKey.toByteArray(Charsets.UTF_8)))
    }

    fun decrypt(encryptedApiKey: String, password: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, generateKey(password))
        return String(cipher.doFinal(Base64.getDecoder().decode(encryptedApiKey)))
    }

    /*fun createKeyHash(activity: Activity, yourPackage: String) {
        val info = activity.packageManager.getPackageInfo(yourPackage, PackageManager.GET_SIGNATURES)
        for (signature in info.signatures) {
            val md = MessageDigest.getInstance("SHA")
            md.update(signature.toByteArray())
            Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
        }
    }*/
}

/*
usage
    val apiKey = "your api key"
    val secretKey = "your secret"

    // Encrypt the API key
    val encryptedApiKey = CryptoUtils.encrypt(apiKey, secretKey)
    println("Encrypted API Key: $encryptedApiKey")

    // Decrypt the encrypted API key
    val decryptedApiKey = CryptoUtils.decrypt(encryptedApiKey, secretKey)
    println("Decrypted API Key: $decryptedApiKey")
 */
