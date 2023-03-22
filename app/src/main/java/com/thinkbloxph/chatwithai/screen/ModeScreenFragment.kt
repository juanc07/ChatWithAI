package com.thinkbloxph.chatwithai.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thinkbloxph.chatwithai.R
import com.thinkbloxph.chatwithai.databinding.FragmentModeScreenBinding
import com.thinkbloxph.chatwithai.helper.UIHelper
import com.thinkbloxph.chatwithai.network.viewmodel.UserViewModel

private const val INNER_TAG = "ModeScreenFragment"
class ModeScreenFragment: Fragment() {
    private var _binding: FragmentModeScreenBinding? = null
    private val binding get() = _binding!!
    private val _userViewModel: UserViewModel by activityViewModels()

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var callback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentModeScreenBinding.inflate(inflater, container, false)

        firebaseAuth = Firebase.auth

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            userViewModel = _userViewModel
            modeScreenFragment = this@ModeScreenFragment
        }
    }

    override fun onStart() {
        super.onStart()
        // hide the action back button
        //UIHelper.getInstance().showHideBackButton(false)
    }

    fun goToChat() {
        findNavController().navigate(R.id.action_modeScreenFragment_to_chatScreenFragment)
    }
}