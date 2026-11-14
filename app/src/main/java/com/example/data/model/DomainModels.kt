package com.example.data.model

import com.example.data.local.UserEntity

data class CommentUiModel(
    val id: String,
    val postId: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarUri: String?,
    val authorVerified: Boolean,
    val text: String,
    val createdAt: Long
)

data class PollOptionUiModel(
    val index: Int,
    val label: String,
    val votesCount: Int,
    val percentage: Float,
    val isSelectedByMe: Boolean
)

data class PostUiModel(
    val id: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorAvatarUri: String?,
    val authorBadge: String,
    val authorVerified: Boolean,
    val authorFrameStyle: String,
    val caption: String,
    val hashtags: List<String>,
    val mediaUris: List<String>,
    val postType: String,
    val isAnonymousConfession: Boolean,
    val isPrivateCircle: Boolean,
    val pollQuestion: String,
    val pollOptions: List<PollOptionUiModel>,
    val totalPollVotes: Int,
    val likesCount: Int,
    val commentsCount: Int,
    val isLikedByMe: Boolean,
    val isSavedByMe: Boolean,
    val isFollowingAuthor: Boolean,
    val createdAt: Long
)

data class StoryItemUiModel(
    val id: String,
    val authorId: String,
    val mediaUri: String,
    val caption: String,
    val isCloseFriends: Boolean,
    val isViewedByMe: Boolean,
    val viewsCount: Int,
    val viewerNames: List<String>,
    val createdAt: Long,
    val expiresAt: Long
)

data class StoryGroupUiModel(
    val author: UserEntity,
    val stories: List<StoryItemUiModel>,
    val hasUnseen: Boolean
)

data class ReelUiModel(
    val id: String,
    val author: UserEntity,
    val mediaUri: String,
    val title: String,
    val caption: String,
    val hashtags: List<String>,
    val durationLabel: String,
    val viewsCount: Long,
    val likesCount: Int,
    val isLikedByMe: Boolean,
    val isFollowingAuthor: Boolean,
    val createdAt: Long
)

data class ConversationUiModel(
    val id: String,
    val title: String,
    val avatarUri: String?,
    val isGroup: Boolean,
    val isOnline: Boolean,
    val otherUserId: String?,
    val memberCount: Int,
    val lastMessageText: String,
    val lastMessageAt: Long,
    val unreadCount: Int
)

data class MessageUiModel(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val senderAvatarUri: String?,
    val isMine: Boolean,
    val text: String,
    val imageUri: String?,
    val reactionEmoji: String?,
    val isRead: Boolean,
    val createdAt: Long
)

data class NotificationUiModel(
    val id: String,
    val actorId: String,
    val actorName: String,
    val actorUsername: String,
    val actorAvatarUri: String?,
    val type: String,
    val targetId: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: Long
)

data class UserSummaryUiModel(
    val user: UserEntity,
    val followersCount: Int,
    val followingCount: Int,
    val postsCount: Int,
    val followStatus: String?, // null, "ACCEPTED", "PENDING"
    val isBlockedByMe: Boolean
)
