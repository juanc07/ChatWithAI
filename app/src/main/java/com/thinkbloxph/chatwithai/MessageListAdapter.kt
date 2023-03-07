package com.thinkbloxph.chatwithai

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class MessageListAdapter(private val messageListRecyclerView: RecyclerView) : RecyclerView.Adapter<MessageViewHolder>() {
    private val messageList = mutableListOf<ChatMessage>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val itemView = when (viewType) {
            USER_MESSAGE_TYPE -> inflater.inflate(R.layout.user_message_item, parent, false)
            AI_MESSAGE_TYPE -> inflater.inflate(R.layout.ai_message_item, parent, false)
            else -> throw IllegalArgumentException("Invalid view type")
        }
        return MessageViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messageList[position]
        holder.bind(message)
    }

    override fun getItemCount() = messageList.size

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        return if (message.sender == "me") {
            USER_MESSAGE_TYPE
        } else {
            AI_MESSAGE_TYPE
        }
    }

    fun addMessage(message: ChatMessage) {
        messageList.add(message)
        notifyItemInserted(messageList.size - 1)
        messageListRecyclerView.scrollToPosition(messageList.size - 1)
    }

    fun clearMessages() {
        messageList.clear()
    }

    companion object {
        const val USER_MESSAGE_TYPE = 1
        const val AI_MESSAGE_TYPE = 2
    }
}
