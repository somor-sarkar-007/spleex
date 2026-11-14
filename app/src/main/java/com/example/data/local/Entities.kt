package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["username"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val fullName: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val bio: String = "",
    val website: String = "",
    val avatarUri: String? = null,
    val coverUri: String? = null,
    val accentHex: String = "#FF75A0",
    val frameStyle: String = "NEON_PINK",
    val badgeLabel: String = "Creator",
    val isPrivate: Boolean = false,
    val isVerified: Boolean = false,
    val isOnline: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "posts",
    indices = [Index(value = ["authorId"]), Index(value = ["createdAt"])]
)
data class PostEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val caption: String,
    val hashtags: String = "",
    val mediaUrisSerialized: String = "",
    val postType: String = "PHOTO", // PHOTO, CAROUSEL, POLL, CONFESSION
    val isAnonymousConfession: Boolean = false,
    val isPrivateCircle: Boolean = false,
    val pollQuestion: String = "",
    val pollOptionsSerialized: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "likes",
    primaryKeys = ["postId", "userId"]
)
data class LikeEntity(
    val postId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "saved_posts",
    primaryKeys = ["postId", "userId"]
)
data class SavedPostEntity(
    val postId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "poll_votes",
    primaryKeys = ["postId", "userId"]
)
data class PollVoteEntity(
    val postId: String,
    val userId: String,
    val optionIndex: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "comments",
    indices = [Index(value = ["postId"])]
)
data class CommentEntity(
    @PrimaryKey val id: String,
    val postId: String,
    val authorId: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "follows",
    primaryKeys = ["followerId", "followingId"]
)
data class FollowEntity(
    val followerId: String,
    val followingId: String,
    val status: String = "ACCEPTED", // ACCEPTED or PENDING
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "blocked_users",
    primaryKeys = ["blockerId", "blockedId"]
)
data class BlockEntity(
    val blockerId: String,
    val blockedId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stories",
    indices = [Index(value = ["authorId"]), Index(value = ["expiresAt"])]
)
data class StoryEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val mediaUri: String,
    val caption: String = "",
    val isCloseFriends: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
)

@Entity(
    tableName = "story_views",
    primaryKeys = ["storyId", "viewerId"]
)
data class StoryViewEntity(
    val storyId: String,
    val viewerId: String,
    val reactionEmoji: String? = null,
    val viewedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reels",
    indices = [Index(value = ["authorId"])]
)
data class ReelEntity(
    @PrimaryKey val id: String,
    val authorId: String,
    val mediaUri: String,
    val title: String,
    val caption: String,
    val hashtags: String = "",
    val durationLabel: String = "0:18",
    val viewsCount: Long = 120L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "reel_likes",
    primaryKeys = ["reelId", "userId"]
)
data class ReelLikeEntity(
    val reelId: String,
    val userId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String = "",
    val isGroup: Boolean = false,
    val lastMessageText: String = "",
    val lastMessageAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "conversation_members",
    primaryKeys = ["conversationId", "userId"]
)
data class ConversationMemberEntity(
    val conversationId: String,
    val userId: String
)

@Entity(
    tableName = "messages",
    indices = [Index(value = ["conversationId"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val text: String,
    val imageUri: String? = null,
    val reactionEmoji: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "notifications",
    indices = [Index(value = ["recipientId"])]
)
data class NotificationEntity(
    @PrimaryKey val id: String,
    val recipientId: String,
    val actorId: String,
    val type: String, // LIKE, COMMENT, FOLLOW, FOLLOW_REQUEST, STORY_REPLY, MESSAGE
    val targetId: String = "",
    val message: String,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
