package com.thinkbloxph.chatwithai

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.thinkbloxph.chatwithai.screen.TypingStatusListener

class MessageListAdapter(private val messageListRecyclerView: RecyclerView) : RecyclerView.Adapter<MessageViewHolder>() {
    private val messageList = mutableListOf<ChatMessage>()
    var typingStatusListener: TypingStatusListener? = null // Add this property

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

        if (holder.itemViewType == AI_MESSAGE_TYPE) {
            holder.typingStatusListener = object : TypingStatusListener {
                override fun onTypingStatusChanged(isTyping: Boolean) {
                    // Notify the listener that the typing status has changed
                    typingStatusListener?.onTypingStatusChanged(isTyping)
                    if(!isTyping){
                        val layoutManager = messageListRecyclerView.layoutManager as LinearLayoutManager
                        layoutManager.scrollToPosition(messageList.size - 1)
                    }
                }
            }
        }
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

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        // Cancel any running simulations
        for (i in 0 until messageListRecyclerView.childCount) {
            val child = messageListRecyclerView.getChildAt(i)
            val viewHolder = messageListRecyclerView.getChildViewHolder(child) as MessageViewHolder
            viewHolder.cancelSimulation()
        }
        super.onDetachedFromRecyclerView(recyclerView)
    }

    fun addMessage(message: ChatMessage) {
        messageList.add(message)
        notifyItemInserted(messageList.size - 1)
        messageListRecyclerView.scrollToPosition(messageList.size - 1)
    }

    fun clearMessages() {
        messageList.clear()
        notifyDataSetChanged()
    }

    companion object {
        const val USER_MESSAGE_TYPE = 1
        const val AI_MESSAGE_TYPE = 2
    }
}
