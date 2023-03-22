package com.thinkbloxph.chatwithai

import android.content.Intent
import android.os.Handler
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thinkbloxph.chatwithai.screen.TypingStatusListener

class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    //var isTyping: Boolean = false
    lateinit var handler:Handler
    lateinit var runnable:Runnable
    var typingStatusListener: TypingStatusListener? = null // Add this property
    private val shareButton: Button? = itemView.findViewById(R.id.share_button)

    fun bind(message: ChatMessage) {

        // Show/hide the share button depending on the message sender
        if (message.sender == "me") {
            shareButton?.visibility = View.GONE // Hide the share button for user messages
        } else {
            shareButton?.visibility = View.VISIBLE // Show the share button for AI messages
        }

        val messageTextView: TextView = itemView.findViewById(R.id.message_text_view)

        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view)

        if(!message.simulateTyping || message.sender=="me"){
            messageTextView.text = message.text
            nameTextView.text = message.sender
        }else{
            simulateTypingAnimation(messageTextView,message.text)
            nameTextView.text = message.sender
        }

        // Set the share button click listener
        shareButton?.setOnClickListener {
            // TODO: Handle the share button click event
            // For example, you can create an Intent to share the message text using a share sheet:
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message.text)
            }
            itemView.context.startActivity(Intent.createChooser(intent, "Share message via..."))
        }

        // Make links clickable
        messageTextView.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun simulateTypingAnimation(messageTextView: TextView, message: String) {
        //isTyping = true

        val delayMillis = 100L // The delay between each character being displayed, in milliseconds
        val typingMessage = message
        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                messageTextView.text = typingMessage.substring(0, messageTextView.length() + 1)
                if (messageTextView.length() < typingMessage.length) {
                    handler.postDelayed(this, delayMillis)
                } else {
                    // Once the typing animation is complete, you can remove the message
                    messageTextView.text = message
                    //isTyping = false
                    // Notify the listener that typing has stopped
                    typingStatusListener?.onTypingStatusChanged(false)
                }
            }
        }
        handler.postDelayed(runnable, delayMillis)
        // Notify the listener that typing has started
        typingStatusListener?.onTypingStatusChanged(true)
    }

    fun cancelSimulation() {
        //isTyping = false
        handler.removeCallbacks(runnable)
        // Notify the listener that typing has stopped
        typingStatusListener?.onTypingStatusChanged(false)
    }

}
