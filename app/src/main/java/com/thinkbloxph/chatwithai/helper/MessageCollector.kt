package com.thinkbloxph.chatwithai.helper

object MessageCollector {
    private val messages = mutableListOf<String>()

    fun addMessage(message: String) {
        messages.add(message)
    }

    fun getPreviousMessages(): String {
        return messages.joinToString("\n")
    }

    fun clearMessages() {
        messages.clear()
    }
}
