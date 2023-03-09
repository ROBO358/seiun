package io.github.akiomik.seiun.model

import com.squareup.moshi.Json
import java.time.Instant
import java.time.format.DateTimeFormatter

data class FeedPostRecord(
    val text: String,
    @Json(name = "\$type")
    val type: String = "app.bsky.feed.post",
    val createdAt: String,
    val reply: CreateReplyRef? = null,
    val embed: EmbedImagesOrEmbedExternal? = null,
//    val entities: List<Entity>?
) {
    fun createdAtAsInstant(): Instant {
        return Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(createdAt))
    }
}