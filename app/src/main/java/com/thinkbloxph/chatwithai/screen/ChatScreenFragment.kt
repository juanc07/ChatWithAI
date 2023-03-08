package com.thinkbloxph.chatwithai.screen

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.login.LoginManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thinkbloxph.chatwithai.*
import com.thinkbloxph.chatwithai.api.GoogleApi
import com.thinkbloxph.chatwithai.databinding.ActivityMainBinding
import com.thinkbloxph.chatwithai.databinding.FragmentChatScreenBinding
import com.thinkbloxph.chatwithai.helper.UIHelper
import com.thinkbloxph.chatwithai.network.UserDatabase
import com.thinkbloxph.chatwithai.network.viewmodel.UserViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val INNER_TAG = "ChatScreenFragment"
class ChatScreenFragment: Fragment() {
    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!
    private val _userViewModel: UserViewModel by activityViewModels()

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var progressDialog: CustomCircularProgressIndicator
    private lateinit var messageListAdapter: MessageListAdapter
    private lateinit var sendButton: Button
    private lateinit var messageInputField: TextInputEditText
    private val userDb = UserDatabase()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatScreenBinding.inflate(inflater, container, false)
        firebaseAuth = Firebase.auth

        progressDialog = CustomCircularProgressIndicator(requireActivity())
        messageListAdapter = MessageListAdapter(binding.messageListRecyclerView)
        binding.messageListRecyclerView.adapter = messageListAdapter
        binding.messageListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set other view references using binding
        messageInputField = binding.messageInputField
        sendButton = binding.sendButton

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        // Access other views using binding here
        sendButton.setOnClickListener {

            var remainingCredit = _userViewModel.getCredit()
            Log.d(TAG, "[${INNER_TAG}]: check credit: ${remainingCredit}}!")
            if (remainingCredit != null) {
                if(remainingCredit > 0){
                    // Send button click logic here
                    val messageText = messageInputField.text.toString()
                    if (!messageText.isNullOrEmpty() && !messageText.isBlank()) {
                        sendButton.isEnabled = false
                        val message = ChatMessage(messageText, "me")
                        messageListAdapter.addMessage(message)
                        messageInputField.text?.clear()
                        progressDialog.show()


                        // Auto-reply from AI after user sends a message
                        /*val aiResponseText =
                            "rd: ${generateRandomString()}"
                        Handler(Looper.getMainLooper()).postDelayed({
                            val aiResponse = ChatMessage(aiResponseText, "AI")
                            messageListAdapter.addMessage(aiResponse)
                        }, 2000) // delay AI response by 2 seconds*/


                        val openAI = OpenAIAPI(lifecycleScope,requireContext())
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val messages = openAI.getCompletion(messageText)
                                // Update UI with messages
                                withContext(Dispatchers.Main) {
                                    //println(messages)
                                    if (messages.isNotEmpty()) {
                                        // The messages are not empty
                                        // Do something with the messages here
                                        val firstMessage = messages[0].trim()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            progressDialog.hide()
                                            val aiResponse = ChatMessage(firstMessage, "AI")
                                            messageListAdapter.addMessage(aiResponse)
                                            sendButton.isEnabled = true

                                            _userViewModel.getCredit()?.let { it1 ->
                                                userDb.updateCredit(it1,1, callback = { newCredit,isSuccess->
                                                    if(isSuccess){
                                                        _userViewModel.setCredit(newCredit)
                                                        Log.d(TAG, "[${INNER_TAG}]: deduct credit success!")
                                                    }else{
                                                        Log.d(TAG, "[${INNER_TAG}]: deduct credit failed!")
                                                    }
                                                })
                                            }

                                        }, 500) // delay AI response by 2 seconds
                                    } else {
                                        // The messages are empty
                                        // Handle this case here
                                        MaterialAlertDialogBuilder(requireContext())
                                            .setTitle("Oops!")
                                            .setMessage("Something went wrong. Please try again later.")
                                            .setPositiveButton("OK") { dialog, _ ->
                                                sendButton.isEnabled = true
                                                dialog.dismiss()
                                            }
                                            .show()
                                    }
                                }
                            } catch (e: Exception) {
                                // Show dialog to notify user of error
                                withContext(Dispatchers.Main) {
                                    MaterialAlertDialogBuilder(requireContext())
                                        .setTitle("Oops!")
                                        .setMessage("Something went wrong. Please try again later.")
                                        .setPositiveButton("OK") { dialog, _ ->
                                            sendButton.isEnabled = true
                                            dialog.dismiss()
                                        }
                                        .show()
                                }
                            }
                        }

                    }else if( messageText.contains(" ")){
                        showDialog("Hey Buddy","Please enter a message!")
                    }
                    else {
                        // The messages are empty
                        // Handle this case here
                        showDialog("Hey Buddy","Please enter a message!")
                    }
                }else {
                    // The messages are empty
                    // Handle this case here
                    showDialog("Sorry Buddy",getString(R.string.out_of_credit_info))
                }
            }else{
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Oops!")
                    .setMessage("Something went wrong. Credit not found. Please try again later.")
                    .setPositiveButton("OK") { dialog, _ ->
                        sendButton.isEnabled = true
                        dialog.dismiss()
                    }
                    .show()
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun showDialog(title:String, message:String){
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                sendButton.isEnabled = true
                dialog.dismiss()
            }
            .show()
    }

    private fun generateRandomString(): String {
        val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..10)
            .map { kotlin.random.Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    private fun showHideBottomNavigation(isShow: Boolean) {
        val bottomNav =
            requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        if (isShow) {
            bottomNav?.visibility = View.VISIBLE
        } else {
            bottomNav?.visibility = View.GONE
        }
    }

    private fun showHideSideNavigation(isShow: Boolean) {
        val sideNavView = requireActivity().findViewById<NavigationView>(R.id.nav_view)
        if (isShow) {
            sideNavView?.visibility = View.VISIBLE
        } else {
            sideNavView?.visibility = View.GONE
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            userViewModel = _userViewModel
            chatScreenFragment = this@ChatScreenFragment
        }
    }

    override fun onStart() {
        super.onStart()
        // hide the action back button
        //UIHelper.getInstance().showHideBackButton(true)
        UIHelper.getInstance().showHideActionBarWithoutBackButton(true,(requireActivity() as MainActivity).binding)
        showHideBottomNavigation(false)
        showHideSideNavigation(false)
    }

    fun goToChat() {
        //findNavController().navigate(R.id.action_loginScreenFragment_to_chatActivity)
    }

    fun clearInput(){
        messageInputField.text?.clear()
    }

    fun signout(){
        var currentProvider: String? = null
        if (firebaseAuth.currentUser?.providerData?.size!! > 0) {
            //Prints Out google.com for Google Sign In, prints facebook.com for Facebook
            currentProvider =  firebaseAuth.currentUser!!.providerData.get(firebaseAuth.currentUser!!.providerData.size - 1).providerId
            Log.v(TAG, "[${INNER_TAG}]: currentProvider ${currentProvider}")
        }

        when(currentProvider){
            "facebook.com" -> {
                LoginManager.getInstance().logOut();
                Log.v(TAG, "[${INNER_TAG}]: facebook SignOut success!")
            }
            "google.com" -> {
                GoogleApi.getInstance()?.signOut {
                    Log.v(TAG, "[${INNER_TAG}]: google SignOut success!")
                }
            }
            else ->{
                Log.v(TAG, "[${INNER_TAG}]: provider not detected!")
            }
        }
        firebaseAuth.signOut()
        //findNavController().navigate(R.id.action_dashboardScreenFragment_to_welcomeScreenFragment)
    }
}