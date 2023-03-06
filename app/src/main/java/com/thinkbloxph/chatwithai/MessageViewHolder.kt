package com.thinkbloxph.chatwithai

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bind(message: ChatMessage) {
        val messageTextView: TextView = itemView.findViewById(R.id.message_text_view)
        val nameTextView: TextView = itemView.findViewById(R.id.name_text_view)

        messageTextView.text = message.text
        nameTextView.text = message.sender
    }
}
