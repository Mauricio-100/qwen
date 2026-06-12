package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class User(
    val id: String = "",
    val username: String = "",
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val email: String? = null,
    @Json(name = "phone_number") val phoneNumber: String? = null,
    @Json(name = "birth_date") val birthDate: String? = null,
    @Json(name = "is_online") val isOnline: Boolean = false,
    @Json(name = "is_verified") val isVerified: Boolean = false,
    val badge: Boolean = false,
    @Json(name = "zodiac_sign") val zodiacSign: String? = null,
    @Json(name = "followers_count") val followersCount: Int = 0,
    @Json(name = "following_count") val followingCount: Int = 0,
    @Json(name = "videos_count") val videosCount: Int = 0,
    @Json(name = "likes_received") val likesReceived: Int = 0,
    @Json(name = "is_following") val isFollowing: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Video(
    val id: String = "",
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    val description: String = "",
    val likes: Int = 0,
    val views: Int = 0,
    @Json(name = "comments_count") val commentsCount: Int = 0,
    @Json(name = "shares_count") val sharesCount: Int = 0,
    val duration: Float? = null,
    @Json(name = "user_id") val userId: String = "",
    val username: String = "",
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "is_verified") val isVerified: Boolean = false,
    val liked: Boolean = false,
    @Json(name = "is_following") val isFollowing: Boolean = false,
    @Json(name = "audio_title") val audioTitle: String? = "Original Sound",
    @Json(name = "audio_owner") val audioOwner: String? = null
)

@JsonClass(generateAdapter = true)
data class Message(
    val id: String,
    val content: String,
    val type: String,
    @Json(name = "sender_id") val senderId: String,
    @Json(name = "receiver_id") val receiverId: String,
    val read: Boolean,
    @Json(name = "sender_username") val senderUsername: String,
    @Json(name = "sender_avatar") val senderAvatar: String? = null,
    @Json(name = "is_verified") val isVerified: Boolean = false
)

@JsonClass(generateAdapter = true)
data class LiveStream(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val username: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val title: String,
    val description: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "viewer_count") val viewerCount: Int = 0,
    @Json(name = "is_live") val isLive: Boolean = false,
    @Json(name = "is_private") val isPrivate: Boolean = false,
    @Json(name = "stream_key") val streamKey: String? = null
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "user_id") val userId: String,
    val username: String,
    @Json(name = "avatar_url") val avatarUrl: String?,
    @Json(name = "is_verified") val isVerified: Boolean
)

@JsonClass(generateAdapter = true)
data class Conversation(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val username: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "last_message") val lastMessage: String? = null,
    @Json(name = "unread_count") val unreadCount: Int = 0,
    @Json(name = "is_online") val isOnline: Boolean = false,
    @Json(name = "is_verified") val isVerified: Boolean = false
)

@JsonClass(generateAdapter = true)
data class ActFile(
    val id: String = "",
    val content: String = "",
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "views_count") val viewsCount: Int = 0,
    @Json(name = "replies_count") val repliesCount: Int = 0,
    @Json(name = "user_id") val userId: String = "",
    val username: String = "",
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "is_verified") val isVerified: Boolean = false,
    val liked: Boolean = false
)

@JsonClass(generateAdapter = true)
data class VerificationCriteria(
    @Json(name = "birth_date") val birthDate: String
)

@JsonClass(generateAdapter = true)
data class VerificationResult(
    val verified: Boolean,
    val badge: Boolean,
    val reason: String? = null,
    @Json(name = "zodiac_sign") val zodiacSign: String? = null
)

@JsonClass(generateAdapter = true)
data class VerificationCriteriaData(
    @Json(name = "min_followers") val minFollowers: Int,
    @Json(name = "min_total_views") val minTotalViews: Int,
    @Json(name = "min_age") val minAge: Int
)

@JsonClass(generateAdapter = true)
data class VerificationStatusResponse(
    @Json(name = "is_verified") val isVerified: Boolean,
    @Json(name = "zodiac_sign") val zodiacSign: String?,
    @Json(name = "verified_at") val verifiedAt: String?,
    @Json(name = "followers_count") val followersCount: Int,
    @Json(name = "total_views") val totalViews: Int,
    val criteria: VerificationCriteriaData
)

@JsonClass(generateAdapter = true)
data class Story(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    val username: String = "",
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "media_url") val mediaUrl: String = "",
    @Json(name = "is_verified") val isVerified: Boolean = false,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class VideoComment(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val username: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val content: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "is_verified") val isVerified: Boolean = false
)

@JsonClass(generateAdapter = true)
data class Notification(
    val id: String,
    val type: String,
    @Json(name = "from_user_id") val fromUserId: String?,
    @Json(name = "from_username") val fromUsername: String?,
    @Json(name = "from_avatar") val fromAvatar: String?,
    @Json(name = "target_id") val targetId: String?,
    val message: String,
    val read: Boolean,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ActFileReply(
    val id: String,
    @Json(name = "user_id") val userId: String,
    val username: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val content: String,
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "is_verified") val isVerified: Boolean = false
)
