package com.thinkbloxph.chatwithai

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatActivity : AppCompatActivity() {
    private lateinit var messageListRecyclerView: RecyclerView
    private lateinit var messageInputField: EditText
    private lateinit var sendButton: Button
    private lateinit var messageListAdapter: MessageListAdapter
    private lateinit var progressBar: CircularProgressIndicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        messageListRecyclerView = findViewById(R.id.message_list_recycler_view)
        messageInputField = findViewById(R.id.message_input_field)
        sendButton = findViewById(R.id.send_button)
        progressBar = findViewById(R.id.progress_bar)

        messageListAdapter = MessageListAdapter(messageListRecyclerView)
        messageListRecyclerView.adapter = messageListAdapter
        messageListRecyclerView.layoutManager = LinearLayoutManager(this)

        sendButton.setOnClickListener {
            val messageText = messageInputField.text.toString()
            if (!messageText.isNullOrEmpty() && !messageText.isBlank()) {
                sendButton.isEnabled = false
                val message = ChatMessage(messageText, "me")
                messageListAdapter.addMessage(message)
                messageInputField.text.clear()
                progressBar.show()


                // Auto-reply from AI after user sends a message
                /*val aiResponseText =
                    "rd: ${generateRandomString()}"
                Handler(Looper.getMainLooper()).postDelayed({
                    val aiResponse = ChatMessage(aiResponseText, "AI")
                    messageListAdapter.addMessage(aiResponse)
                }, 2000) // delay AI response by 2 seconds*/


                val openAI = OpenAIAPI(lifecycleScope,applicationContext)
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
                                    progressBar.hide()
                                    val aiResponse = ChatMessage(firstMessage, "AI")
                                    messageListAdapter.addMessage(aiResponse)
                                    sendButton.isEnabled = true
                                }, 2000) // delay AI response by 2 seconds
                            } else {
                                // The messages are empty
                                // Handle this case here
                                MaterialAlertDialogBuilder(this@ChatActivity)
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
                            MaterialAlertDialogBuilder(this@ChatActivity)
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
    }

    private fun showDialog(title:String, message:String){
        MaterialAlertDialogBuilder(this@ChatActivity)
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
}

