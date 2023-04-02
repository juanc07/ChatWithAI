package com.thinkbloxph.chatwithai.helper

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thinkbloxph.chatwithai.TAG
import java.io.File
import java.io.IOException

private const val INNER_TAG = "AudioRecorder"
class AudioRecorder(val _application: Application, val _activity: Activity, val _fragment: Fragment) {

    companion object {
        private var instance: AudioRecorder? = null

        fun initInstance(_application: Application, _activity: Activity, _fragment: Fragment) {
            instance = AudioRecorder(_application, _activity, _fragment)
        }

        fun getInstance(): AudioRecorder? {
            return instance
        }
    }

    private lateinit var _context: Context
    private var recorder: MediaRecorder? = null
    private var filePath: String? = null
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    fun init() {
        _context = _application?.getApplicationContext()!!

        requestPermissionLauncher =
            _fragment.requireActivity()
                .registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions())
                { result ->
                    Log.d(
                        TAG,
                        "[${INNER_TAG}]: registerForActivityResult  result ${result.toString()}"
                    )
                    var allAreGranted = true
                    for (b in result.values) {
                        allAreGranted = allAreGranted && b
                    }
                    if (allAreGranted) {
                        Log.d(TAG, "[${INNER_TAG}]: registerForActivityResult  allAreGranted")
                        // do something when all permission is granted!!
                    } else {
                        Log.d(TAG, "[${INNER_TAG}]: registerForActivityResult  denied")
                        // Explain to the user that the feature is unavailable because the
                        // feature requires a permission that the user has denied. At the
                        // same time, respect the user's decision. Don't link to system
                        // settings in an effort to convince the user to change their
                        // decision.
                        Log.d(TAG, "[${INNER_TAG}]:  getPhoneNumber denied")
                        Log.d(
                            TAG,
                            "[${INNER_TAG}]:  check if we can still show them dialog why we need it"
                        )
                        if (ActivityCompat.shouldShowRequestPermissionRationale(
                                _activity,
                                Manifest.permission.RECORD_AUDIO
                            )
                        ) {
                            val builder = MaterialAlertDialogBuilder(_activity)
                            builder.setMessage("This app requires the RECORD_AUDIO permission to record your voice. Without this permission, the app will not be able to function as expected.")
                                .setPositiveButton("OK") { _, _ ->
                                    requestPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                }
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                }
                            val alert = builder.create()
                            alert.show()
                        } else {
                            Log.d(
                                TAG,
                                "[${INNER_TAG}]: registerForActivityResult  The user has chosen \"Don't show again\" or the permission has already been granted"
                            )
                            // The user has chosen "Don't show again" or the permission has already been granted
                            // Do something here
                            // do something when all permission is not granted!!
                        }
                    }
                }

        Log.i(INNER_TAG, "AudioRecorder init!")
    }

    fun checkPermission():Boolean{
        if (ActivityCompat.checkSelfPermission(
                _context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.RECORD_AUDIO
                )
            )
           return false
        }
        return true
    }

    @Throws(IOException::class)
    fun startRecording(context: Context) {
        val audioFile = createAudioFile(_context)
        recorder = MediaRecorder()
        recorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioChannels(1)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(192000)
            setOutputFile(audioFile.absolutePath)
            prepare()
            start()
        }
        filePath = audioFile.absolutePath
    }

    fun stopRecording(): String? {
        recorder?.apply {
            stop()
            release()
        }
        recorder = null
        return filePath
    }

    private fun createAudioFile(context: Context): File {
        val audioDir = _context.getExternalFilesDir(null)
        return File.createTempFile("audio", ".mp3", audioDir)
    }
}
