package com.thinkbloxph.chatwithai

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.thinkbloxph.chatwithai.databinding.ProgressDialogBinding

class CustomCircularProgressIndicator(private val activity: Activity) {

    private var progressIndicator: CircularProgressIndicator? = null
    private var progressIndicatorBinding: ProgressDialogBinding? = null

    fun show() {
        progressIndicatorBinding = DataBindingUtil.inflate(
            LayoutInflater.from(activity),
            R.layout.progress_dialog,
            null,
            false
        )
        progressIndicator = progressIndicatorBinding?.progressBar
        progressIndicatorBinding?.let {
            (activity.window.decorView as ViewGroup).addView(it.root)

            it.progressBar.visibility = View.VISIBLE

            activity.window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    fun hide() {
        progressIndicatorBinding?.let {
            (activity.window.decorView as ViewGroup).removeView(it.root)
            it.progressBar.visibility = View.INVISIBLE
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }
}