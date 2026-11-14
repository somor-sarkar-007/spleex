package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpleexDao {
    // Users
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun observeAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUserById(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun countUsers(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Update
    suspend fun updateUser(user: UserEntity)

    // Posts
    @Query("SELECT * FROM posts ORDER BY createdAt DESC")
    fun observeAllPosts(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePost(postId: String)

    // Likes
    @Query("SELECT * FROM likes")
    fun observeAllLikes(): Flow<List<LikeEntity>>

    @Query("SELECT * FROM likes WHERE postId = :postId AND userId = :userId LIMIT 1")
    suspend fun getLike(postId: String, userId: String): LikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: LikeEntity)

    @Query("DELETE FROM likes WHERE postId = :postId AND userId = :userId")
    suspend fun deleteLike(postId: String, userId: String)

    // Saved Posts
    @Query("SELECT * FROM saved_posts WHERE userId = :userId")
    fun observeSavedPosts(userId: String): Flow<List<SavedPostEntity>>

    @Query("SELECT * FROM saved_posts WHERE postId = :postId AND userId = :userId LIMIT 1")
    suspend fun getSavedPost(postId: String, userId: String): SavedPostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPost(saved: SavedPostEntity)

    @Query("DELETE FROM saved_posts WHERE postId = :postId AND userId = :userId")
    suspend fun deleteSavedPost(postId: String, userId: String)

    // Poll Votes
    @Query("SELECT * FROM poll_votes")
    fun observeAllPollVotes(): Flow<List<PollVoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPollVote(vote: PollVoteEntity)

    // Comments
    @Query("SELECT * FROM comments ORDER BY createdAt ASC")
    fun observeAllComments(): Flow<List<CommentEntity>>

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY createdAt ASC")
    fun observeCommentsForPost(postId: String): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComments(comments: List<CommentEntity>)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteComment(commentId: String)

    // Follows
    @Query("SELECT * FROM follows")
    fun observeAllFollows(): Flow<List<FollowEntity>>

    @Query("SELECT * FROM follows WHERE followerId = :followerId AND followingId = :followingId LIMIT 1")
    suspend fun getFollow(followerId: String, followingId: String): FollowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollow(follow: FollowEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollows(follows: List<FollowEntity>)

    @Query("DELETE FROM follows WHERE followerId = :followerId AND followingId = :followingId")
    suspend fun deleteFollow(followerId: String, followingId: String)

    // Blocks
    @Query("SELECT * FROM blocked_users WHERE blockerId = :blockerId")
    fun observeBlockedByUser(blockerId: String): Flow<List<BlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: BlockEntity)

    @Query("DELETE FROM blocked_users WHERE blockerId = :blockerId AND blockedId = :blockedId")
    suspend fun deleteBlock(blockerId: String, blockedId: String)

    // Stories
    @Query("SELECT * FROM stories WHERE expiresAt > :now ORDER BY createdAt DESC")
    fun observeActiveStories(now: Long): Flow<List<StoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<StoryEntity>)

    @Query("SELECT * FROM story_views")
    fun observeAllStoryViews(): Flow<List<StoryViewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoryView(view: StoryViewEntity)

    // Reels
    @Query("SELECT * FROM reels ORDER BY createdAt DESC")
    fun observeAllReels(): Flow<List<ReelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReel(reel: ReelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReels(reels: List<ReelEntity>)

    @Query("UPDATE reels SET viewsCount = viewsCount + 1 WHERE id = :reelId")
    suspend fun incrementReelViews(reelId: String)

    @Query("SELECT * FROM reel_likes")
    fun observeAllReelLikes(): Flow<List<ReelLikeEntity>>

    @Query("SELECT * FROM reel_likes WHERE reelId = :reelId AND userId = :userId LIMIT 1")
    suspend fun getReelLike(reelId: String, userId: String): ReelLikeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReelLike(like: ReelLikeEntity)

    @Query("DELETE FROM reel_likes WHERE reelId = :reelId AND userId = :userId")
    suspend fun deleteReelLike(reelId: String, userId: String)

    // Messaging
    @Query("SELECT * FROM conversations ORDER BY lastMessageAt DESC")
    fun observeAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversation_members")
    fun observeAllConversationMembers(): Flow<List<ConversationMemberEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun observeMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages ORDER BY createdAt DESC")
    fun observeAllMessages(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversationMembers(members: List<ConversationMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE conversations SET lastMessageText = :text, lastMessageAt = :timestamp WHERE id = :conversationId")
    suspend fun updateConversationPreview(conversationId: String, text: String, timestamp: Long)

    @Query("UPDATE messages SET isRead = 1 WHERE conversationId = :conversationId AND senderId != :readerId")
    suspend fun markConversationMessagesRead(conversationId: String, readerId: String)

    @Query("UPDATE messages SET reactionEmoji = :emoji WHERE id = :messageId")
    suspend fun updateMessageReaction(messageId: String, emoji: String?)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessage(messageId: String)

    // Notifications
    @Query("SELECT * FROM notifications WHERE recipientId = :userId ORDER BY createdAt DESC")
    fun observeNotificationsForUser(userId: String): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1 WHERE recipientId = :userId")
    suspend fun markAllNotificationsRead(userId: String)
}
