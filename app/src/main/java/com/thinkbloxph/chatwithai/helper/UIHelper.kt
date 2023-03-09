package com.thinkbloxph.chatwithai.helper

import android.app.Activity
import android.content.Context
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.thinkbloxph.chatwithai.CustomCircularProgressIndicator
import com.thinkbloxph.chatwithai.TAG
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import androidx.navigation.ui.NavigationUI
import com.thinkbloxph.chatwithai.databinding.ActivityMainBinding


class UIHelper(val _activity: Activity, val _fragment: Fragment) {
    private lateinit var progressDialog: CustomCircularProgressIndicator

    companion object {
        private lateinit var instance: UIHelper

        fun initInstance(_activity: Activity, _fragment: Fragment) {
            instance = UIHelper(_activity, _fragment)
        }

        fun getInstance(): UIHelper {
            return instance
        }
    }

    fun init() {
        progressDialog = CustomCircularProgressIndicator(_activity)
    }

    fun showDialogMessage(message: String, buttonLabel: String) {
        val builder = MaterialAlertDialogBuilder(_activity)
        builder.setMessage(message)
            .setPositiveButton(buttonLabel) { dialog, _ ->
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    fun showDialogWithPositiveNegativeCallback(
        message: String, positiveButtonLabel: String,
        negativeButtonLabel: String,
        positiveCallback: () -> Unit, negativeCallback: () -> Unit
    ) {
        val builder = MaterialAlertDialogBuilder(_activity)
        builder.setMessage(message)
            .setPositiveButton(positiveButtonLabel) { dialog, _ ->
                positiveCallback()
                dialog.dismiss()
            }.setNegativeButton(negativeButtonLabel) { dialog, _ ->
                negativeCallback()
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    fun showDialogWithOneCallback(
        message: String, positiveButtonLabel: String,
        callback: () -> Unit
    ) {
        val builder = MaterialAlertDialogBuilder(_activity)
        builder.setMessage(message)
            .setPositiveButton(positiveButtonLabel) { dialog, _ ->
                callback()
                dialog.dismiss()
            }

        val alert = builder.create()
        alert.show()
    }

    fun showLoading() {
        progressDialog.show()
    }

    fun hideLoading() {
        progressDialog.hide()
    }

    fun hideKeyboard() {
        val imm =
            _fragment.requireActivity()
                ?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(_fragment.requireActivity().currentFocus?.windowToken, 0)
        Log.w(TAG, "hideKeyboard!!!")
    }

    fun makeStringClickable(textView: MaterialTextView) {
        // note: you don't need autoLink = web on layout for this to work
        // and you have to create a string with href html tag for it to work
        textView.movementMethod = LinkMovementMethod.getInstance()
    }

    fun showHideActionBarWithoutBackButton(isShow: Boolean,binding:ActivityMainBinding) {
        if (isShow) {
            (_activity as AppCompatActivity).supportActionBar?.show()
            _activity.supportActionBar?.setDisplayShowHomeEnabled(false)
            binding.toolbar.navigationIcon = null
        }
        else {
            (_activity as AppCompatActivity).supportActionBar?.hide()
            _activity.supportActionBar?.setDisplayShowHomeEnabled(false)
            binding.toolbar.navigationIcon = null
        }
    }

    fun showHideActionBar(isShow: Boolean,binding:ActivityMainBinding) {
        if (isShow) {
            (_activity as AppCompatActivity).supportActionBar?.show()
        }
        else {
            (_activity as AppCompatActivity).supportActionBar?.hide()
        }
    }
}