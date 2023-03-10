package com.thinkbloxph.chatwithai

import android.os.Handler
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thinkbloxph.chatwithai.screen.TypingStatusListener

class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    //var isTyping: Boolean = false
    lateinit var handler:Handler
    lateinit var runnable:Runnable
    var typingStatusListener: TypingStatusListener? = null // Add this property

    fun bind(message: ChatMessage) {
        val messageTextView: TextView = itemView.findViewById(R.id.message_text_view)
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view)

        if( message.sender=="me"){
            messageTextView.text = message.text
            nameTextView.text = message.sender
        }else{
            simulateTypingAnimation(messageTextView,message.text)
            nameTextView.text = message.sender
        }
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
