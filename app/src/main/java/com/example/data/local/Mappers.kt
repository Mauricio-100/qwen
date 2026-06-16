package com.example.data.local

import com.example.data.models.User
import com.example.data.models.Message
import com.example.data.models.Conversation
import com.example.data.models.Video

fun User.toCachedEntity(): CachedUserProfile {
    return CachedUserProfile(
        id = this.id,
        username = this.username,
        avatarUrl = this.avatarUrl,
        bio = this.bio,
        email = this.email,
        phoneNumber = this.phoneNumber,
        birthDate = this.birthDate,
        zodiacSign = this.zodiacSign,
        followersCount = this.followersCount,
        followingCount = this.followingCount,
        videosCount = this.videosCount,
        likesReceived = this.likesReceived,
        isFollowing = this.isFollowing
    )
}

fun CachedUserProfile.toDomainModel(): User {
    return User(
        id = this.id,
        username = this.username,
        avatarUrl = this.avatarUrl,
        bio = this.bio,
        email = this.email,
        phoneNumber = this.phoneNumber,
        birthDate = this.birthDate,
        zodiacSign = this.zodiacSign,
        followersCount = this.followersCount,
        followingCount = this.followingCount,
        videosCount = this.videosCount,
        likesReceived = this.likesReceived,
        isFollowing = this.isFollowing
    )
}

fun Message.toCachedEntity(): CachedMessage {
    return CachedMessage(
        id = this.id,
        content = this.content,
        type = this.type,
        senderId = this.senderId,
        receiverId = this.receiverId,
        read = this.read,
        senderUsername = this.senderUsername,
        senderAvatar = this.senderAvatar,
        isVerified = this.isVerified
    )
}

fun CachedMessage.toDomainModel(): Message {
    return Message(
        id = this.id,
        content = this.content,
        type = this.type,
        senderId = this.senderId,
        receiverId = this.receiverId,
        read = this.read,
        senderUsername = this.senderUsername,
        senderAvatar = this.senderAvatar,
        isVerified = this.isVerified
    )
}

fun Conversation.toCachedEntity(): CachedConversation {
    return CachedConversation(
        id = this.id,
        userId = this.userId,
        username = this.username,
        avatarUrl = this.avatarUrl,
        lastMessage = this.lastMessage,
        unreadCount = this.unreadCount,
        isOnline = this.isOnline,
        isVerified = this.isVerified
    )
}

fun CachedConversation.toDomainModel(): Conversation {
    return Conversation(
        id = this.id,
        userId = this.userId,
        username = this.username,
        avatarUrl = this.avatarUrl,
        lastMessage = this.lastMessage,
        unreadCount = this.unreadCount,
        isOnline = this.isOnline,
        isVerified = this.isVerified
    )
}

fun Video.toCachedEntity(): CachedVideo {
    return CachedVideo(
        id = this.id,
        videoUrl = this.videoUrl,
        thumbnailUrl = this.thumbnailUrl,
        description = this.description,
        username = this.username,
        likes = this.likes,
        views = this.views
    )
}

fun CachedVideo.toDomainModel(): Video {
    return Video(
        id = this.id,
        videoUrl = this.videoUrl,
        thumbnailUrl = this.thumbnailUrl,
        description = this.description,
        username = this.username,
        likes = this.likes,
        views = this.views
    )
}
