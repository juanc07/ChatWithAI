package com.thinkbloxph.chatwithai.screen

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thinkbloxph.chatwithai.*
import com.thinkbloxph.chatwithai.databinding.ActivityMainBinding
import com.thinkbloxph.chatwithai.databinding.FragmentChatScreenBinding
import com.thinkbloxph.chatwithai.helper.UIHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val INNER_TAG = "ChatScreenFragment"
class ChatScreenFragment: Fragment() {
    private var _binding: FragmentChatScreenBinding? = null
    private val binding get() = _binding!!

    private lateinit var progressDialog: CustomCircularProgressIndicator
    private lateinit var messageListAdapter: MessageListAdapter
    private lateinit var sendButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatScreenBinding.inflate(inflater, container, false)


        progressDialog = CustomCircularProgressIndicator(requireActivity())
        messageListAdapter = MessageListAdapter(binding.messageListRecyclerView)
        binding.messageListRecyclerView.adapter = messageListAdapter
        binding.messageListRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Set other view references using binding
        val messageInputField = binding.messageInputField
        sendButton = binding.sendButton

        UIHelper.initInstance(this.requireActivity(), this)
        UIHelper.getInstance()?.init()

        // Access other views using binding here
        sendButton.setOnClickListener {
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
                                }, 2000) // delay AI response by 2 seconds
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            chatScreenFragment = this@ChatScreenFragment
        }
    }

    override fun onStart() {
        super.onStart()
        // hide the action back button
        //UIHelper.getInstance().showHideBackButton(true)
        UIHelper.getInstance().showHideActionBarWithoutBackButton(true,(requireActivity() as MainActivity).binding)
    }

    fun goToChat() {
        //findNavController().navigate(R.id.action_loginScreenFragment_to_chatActivity)
    }
}