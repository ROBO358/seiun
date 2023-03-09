package io.github.akiomik.seiun.repository

import android.util.Log
import com.slack.eithernet.ApiResult
import com.slack.eithernet.response
import io.github.akiomik.seiun.SeiunApplication
import io.github.akiomik.seiun.model.*
import io.github.akiomik.seiun.service.AtpService
import io.github.akiomik.seiun.service.UnauthorizedException
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.time.Instant

class TimelineRepository(private val atpService: AtpService) {
    suspend fun getTimeline(session: Session, before: String? = null): Timeline {
        Log.d(SeiunApplication.TAG, "Get timeline: before = $before")

        return when (val result =
            atpService.getTimeline("Bearer ${session.accessJwt}", before = before)) {
            is ApiResult.Success -> result.value
            is ApiResult.Failure -> when (result) {
                is ApiResult.Failure.HttpFailure -> {
                    if (result.code == 401) {
                        throw UnauthorizedException("Unauthorized: ${result.code} (${result.error})")
                    } else {
                        throw IllegalStateException("HttpError: ${result.code} (${result.error})")
                    }
                }
                else -> throw IllegalStateException("ApiResult.Failure: ${result.response()}")
            }
        }
    }

    suspend fun upvote(session: Session, subject: StrongRef): SetVoteResponse {
        Log.d(SeiunApplication.TAG, "Upvote post: uri = ${subject.uri}, cid = ${subject.cid}")

        val body = SetVoteParam(subject = subject, direction = VoteDirection.up)
        when (val result =
            atpService.upvote(authorization = "Bearer ${session.accessJwt}", body = body)) {
            is ApiResult.Success -> return result.value
            is ApiResult.Failure -> when (result) {
                is ApiResult.Failure.HttpFailure -> {
                    if (result.code == 401) {
                        throw UnauthorizedException("Unauthorized: ${result.code} (${result.error})")
                    } else {
                        throw IllegalStateException("HttpError: ${result.code} (${result.error})")
                    }
                }
                else -> throw IllegalStateException("ApiResult.Failure: ${result.response()}")
            }
        }
    }

    suspend fun cancelVote(session: Session, subject: StrongRef) {
        Log.d(SeiunApplication.TAG, "Cancel vote post: uri = ${subject.uri}, cid = ${subject.cid}")

        val body = SetVoteParam(subject = subject, direction = VoteDirection.none)
        when (val result =
            atpService.upvote(authorization = "Bearer ${session.accessJwt}", body = body)) {
            is ApiResult.Success -> {}
            is ApiResult.Failure -> when (result) {
                is ApiResult.Failure.HttpFailure -> {
                    if (result.code == 401) {
                        throw UnauthorizedException("Unauthorized: ${result.code} (${result.error})")
                    } else {
                        throw IllegalStateException("HttpError: ${result.code} (${result.error})")
                    }
                }
                else -> throw IllegalStateException("ApiResult.Failure: ${result.response()}")
            }
        }
    }

    suspend fun repost(session: Session, subject: StrongRef): CreateRecordResponse {
        Log.d(SeiunApplication.TAG, "Cancel repost: uri = ${subject.uri}, cid = ${subject.cid}")

        val createdAt = Instant.now().toString()
        val record = RepostRecord(subject = subject, createdAt)
        val body = RepostParam(did = session.did, record = record)

        when (val result =
            atpService.repost(authorization = "Bearer ${session.accessJwt}", body = body)) {
            is ApiResult.Success -> return result.value
            is ApiResult.Failure -> when (result) {
                is ApiResult.Failure.HttpFailure -> {
                    if (result.code == 401) {
                        throw UnauthorizedException("Unauthorized: ${result.code} (${result.error})")
                    } else {
                        throw IllegalStateException("HttpError: ${result.code} (${result.error})")
                    }
                }
                else -> throw IllegalStateException("ApiResult.Failure: ${result.response()}")
            }
        }
    }

    suspend fun cancelRepost(session: Session, uri: String) {
        Log.d(SeiunApplication.TAG, "Cancel repost: $uri")

        val rkey = uri.split('/').last()
        val body =
            DeleteRecordParam(did = session.did, rkey = rkey, collection = "app.bsky.feed.repost")

        try {
            atpService.deleteRecord(authorization = "Bearer ${session.accessJwt}", body = body)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                throw UnauthorizedException("Unauthorized: ${e.code()} (${e.message()})")
            } else {
                throw IllegalStateException("HttpError: ${e.code()} (${e.message()})")
            }
        }
    }

    suspend fun createPost(
        session: Session,
        content: String,
        imageCid: String?,
        imageMimeType: String?
    ) {
        Log.d(SeiunApplication.TAG, "Create a post: content = $content")

        val createdAt = Instant.now().toString()
        val embed = if (imageCid != null && imageMimeType != null) {
            val image = Image(image = ImageRef(imageCid, imageMimeType), alt = "")
            EmbedImagesOrEmbedExternal(images = listOf(image), type = "app.bsky.embed.images")
        } else {
            null
        }
        val record = FeedPostRecord(text = content, createdAt = createdAt, embed = embed)
        val body = CreatePostParam(did = session.did, record = record)

        when (val result =
            atpService.createPost(authorization = "Bearer ${session.accessJwt}", body = body)) {
            is ApiResult.Success -> {}
            is ApiResult.Failure -> when (result) {
                is ApiResult.Failure.HttpFailure -> {
                    if (result.code == 401) {
                        throw UnauthorizedException("Unauthorized: ${result.code} (${result.error})")
                    } else {
                        throw IllegalStateException("HttpError: ${result.code} (${result.error})")
                    }
                }
                else -> throw IllegalStateException("ApiResult.Failure: ${result.response()}")
            }
        }
    }

    suspend fun createReply(
        session: Session,
        content: String,
        to: CreateReplyRef,
        imageCid: String?,
        imageMimeType: String?
    ) {
        Log.d(SeiunApplication.TAG, "Create a reply: content = $content, to = $to")

        val createdAt = Instant.now().toString()
        val embed = if (imageCid != null && imageMimeType != null) {
            val image = Image(image = ImageRef(imageCid, imageMimeType), alt = "")
            EmbedImagesOrEmbedExternal(images = listOf(image), type = "app.bsky.embed.images")
        } else {
            null
        }
        val record =
            FeedPostRecord(text = content, createdAt = createdAt, reply = to, embed = embed)
        val body = CreatePostParam(did = session.did, record = record)

        when (val result =
            atpService.createPost(authorization = "Bearer ${session.accessJwt}", body = body)) {
            is ApiResult.Success -> {}
            is ApiResult.Failure -> when (result) {
                is ApiResult.Failure.HttpFailure -> {
                    if (result.code == 401) {
                        throw UnauthorizedException("Unauthorized: ${result.code} (${result.error})")
                    } else {
                        throw IllegalStateException("HttpError: ${result.code} (${result.error})")
                    }
                }
                else -> throw IllegalStateException("ApiResult.Failure: ${result.response()}")
            }
        }
    }

    suspend fun deletePost(session: Session, feedViewPost: FeedViewPost) {
        Log.d(SeiunApplication.TAG, "Delete post: uri = ${feedViewPost.post.uri}")

        val rkey = feedViewPost.post.uri.split('/').last()
        val body =
            DeleteRecordParam(did = session.did, collection = "app.bsky.feed.post", rkey = rkey)
        try {
            atpService.deleteRecord("Bearer ${session.accessJwt}", body)
        } catch (e: HttpException) {
            if (e.code() == 401) {
                throw UnauthorizedException("Unauthorized: ${e.code()} (${e.message()})")
            } else {
                throw IllegalStateException("HttpError: ${e.code()} (${e.message()})")
            }
        }
    }

    suspend fun uploadImage(
        session: Session,
        image: ByteArray,
        mimeType: String,
    ): UploadBlobOutput {
        when (val result =
            atpService.uploadBlob(
                authorization = "Bearer ${session.accessJwt}",
                contentType = mimeType,
                body = image.toRequestBody(),
            )) {
            is ApiResult.Success -> return result.value
            is ApiResult.Failure -> when (result) {
                is ApiResult.Failure.HttpFailure -> {
                    if (result.code == 401) {
                        throw UnauthorizedException("Unauthorized: ${result.code} (${result.error})")
                    } else {
                        throw IllegalStateException("HttpError: ${result.code} (${result.error})")
                    }
                }
                else -> throw IllegalStateException("ApiResult.Failure: ${result.response()}")
            }
        }
    }
}