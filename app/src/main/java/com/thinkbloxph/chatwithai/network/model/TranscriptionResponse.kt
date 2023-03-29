package com.thinkbloxph.chatwithai.network.model

import com.squareup.moshi.Json

data class TranscriptionResponse(
    @Json(name = "audio_duration") val audioDuration: Float,
    @Json(name = "text") val text: String
)
