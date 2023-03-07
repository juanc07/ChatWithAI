package com.thinkbloxph.chatwithai.screen

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.thinkbloxph.chatwithai.R
import com.thinkbloxph.chatwithai.databinding.FragmentWelcomeScreenBinding
import com.thinkbloxph.chatwithai.helper.UIHelper


private const val INNER_TAG = "WelcomeScreenFragment"
class WelcomeScreenFragment: Fragment() {
    private var _binding: FragmentWelcomeScreenBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentWelcomeScreenBinding.inflate(inflater, container, false)

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            welcomeScreenFragment = this@WelcomeScreenFragment
        }
    }

    override fun onStart() {
        super.onStart()
        // hide the action back button
        //UIHelper.getInstance().showHideBackButton(false)
    }

    fun goToLogin() {
        findNavController().navigate(R.id.action_welcomeScreenFragment_to_loginScreenFragment)
    }
}