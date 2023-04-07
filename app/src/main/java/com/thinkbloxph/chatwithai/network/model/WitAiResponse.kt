package com.thinkbloxph.chatwithai.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WitAiResponse(
    @Json(name = "entities") val entities: Map<String, List<Entity>>,
    @Json(name = "text") val text: String
) {
    data class Entity(
        @Json(name = "body") val body: String,
        @Json(name = "confidence") val confidence: Double,
        @Json(name = "from") val from: From,
        @Json(name = "type") val type: String,
        @Json(name = "values") val values: List<Value>
    ) {
        data class From(
            @Json(name = "grain") val grain: String,
            @Json(name = "value") val value: String
        )

        data class Value(
            @Json(name = "from") val from: From,
            @Json(name = "type") val type: String
        )
    }
}
