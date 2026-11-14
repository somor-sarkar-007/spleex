package com.example.data.repository

import android.net.Uri
import com.example.data.local.*
import com.example.data.media.MediaStorageManager
import com.example.data.model.*
import com.example.data.preferences.SessionPreferences
import com.example.data.security.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.UUID

class SpleexRepository(
    private val dao: SpleexDao,
    private val mediaStorage: MediaStorageManager,
    private val sessionPrefs: SessionPreferences
) {
    val currentUserIdFlow: Flow<String?> = sessionPrefs.currentUserId
    val isDarkModeFlow: Flow<Boolean> = sessionPrefs.isDarkMode
    val hasSeenOnboardingFlow: Flow<Boolean> = sessionPrefs.hasSeenOnboarding
    val recentSearchesFlow: Flow<List<String>> = sessionPrefs.recentSearches

    suspend fun setDarkMode(enabled: Boolean) = sessionPrefs.setDarkMode(enabled)
    suspend fun setOnboardingSeen() = sessionPrefs.setOnboardingSeen()
    suspend fun addRecentSearch(query: String) = sessionPrefs.addRecentSearch(query)
    suspend fun clearRecentSearches() = sessionPrefs.clearRecentSearches()

    suspend fun register(
        fullName: String,
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        avatarUri: Uri?
    ): Result<UserEntity> {
        val cleanName = fullName.trim()
        val cleanUsername = username.trim().lowercase().removePrefix("@")
        val cleanEmail = email.trim().lowercase()

        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Please enter your full name."))
        }
        if (!cleanUsername.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            return Result.failure(
                IllegalArgumentException("Username must be 3–20 characters (letters, numbers, underscores).")
            )
        }
        if (!cleanEmail.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            return Result.failure(IllegalArgumentException("Please enter a valid email address."))
        }
        if (password.length < 6) {
            return Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }
        if (password != confirmPassword) {
            return Result.failure(IllegalArgumentException("Passwords do not match."))
        }
        if (dao.getUserByUsername(cleanUsername) != null) {
            return Result.failure(IllegalArgumentException("That username is already taken."))
        }
        if (dao.getUserByEmail(cleanEmail) != null) {
            return Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        val savedAvatar = avatarUri?.let { mediaStorage.persistImageFromUri(it, maxDimension = 720) }
        val newUser = UserEntity(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            fullName = cleanName,
            username = cleanUsername,
            email = cleanEmail,
            passwordHash = PasswordHasher.hashPassword(password),
            bio = "New on SPLEEX — sharing moments & connecting.",
            avatarUri = savedAvatar,
            badgeLabel = "Rising Spark",
            isOnline = true
        )
        dao.insertUser(newUser)
        sessionPrefs.setCurrentUserId(newUser.id)
        return Result.success(newUser)
    }

    suspend fun login(identifier: String, password: String): Result<UserEntity> {
        val clean = identifier.trim().lowercase().removePrefix("@")
        if (clean.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter your email or username and password."))
        }
        val user = if (clean.contains("@")) {
            dao.getUserByEmail(clean)
        } else {
            dao.getUserByUsername(clean)
        } ?: return Result.failure(IllegalArgumentException("Account not found."))

        if (!PasswordHasher.verifyPassword(password, user.passwordHash)) {
            return Result.failure(IllegalArgumentException("Incorrect password. Please try again."))
        }
        dao.updateUser(user.copy(isOnline = true))
        sessionPrefs.setCurrentUserId(user.id)
        return Result.success(user)
    }

    suspend fun loginDemoAccount(): Result<UserEntity> {
        val demo = dao.getUserByUsername("aria_vibe")
            ?: return Result.failure(IllegalStateException("Demo profile unavailable."))
        dao.updateUser(demo.copy(isOnline = true))
        sessionPrefs.setCurrentUserId(demo.id)
        return Result.success(demo)
    }

    suspend fun resetPassword(
        email: String,
        newPassword: String,
        confirmPassword: String
    ): Result<Unit> {
        val cleanEmail = email.trim().lowercase()
        val user = dao.getUserByEmail(cleanEmail)
            ?: return Result.failure(IllegalArgumentException("No account found with that email."))
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        if (newPassword != confirmPassword) {
            return Result.failure(IllegalArgumentException("Passwords do not match."))
        }
        dao.updateUser(user.copy(passwordHash = PasswordHasher.hashPassword(newPassword)))
        return Result.success(Unit)
    }

    suspend fun logout() {
        sessionPrefs.setCurrentUserId(null)
    }

    fun observeCurrentUser(userId: String): Flow<UserEntity?> = dao.observeUserById(userId)

    fun observeAllUsers(): Flow<List<UserEntity>> = dao.observeAllUsers()

    fun observeUserSummaries(currentUserId: String): Flow<List<UserSummaryUiModel>> {
        return combine(
            dao.observeAllUsers(),
            dao.observeAllFollows(),
            dao.observeAllPosts(),
            dao.observeBlockedByUser(currentUserId)
        ) { users, follows, posts, blocks ->
            val blockedIds = blocks.map { it.blockedId }.toSet()
            users.map { user ->
                val followers = follows.count { it.followingId == user.id && it.status == "ACCEPTED" }
                val following = follows.count { it.followerId == user.id && it.status == "ACCEPTED" }
                val postCount = posts.count { it.authorId == user.id && !it.isAnonymousConfession }
                val status = follows.firstOrNull {
                    it.followerId == currentUserId && it.followingId == user.id
                }?.status
                UserSummaryUiModel(
                    user = user,
                    followersCount = followers,
                    followingCount = following,
                    postsCount = postCount,
                    followStatus = status,
                    isBlockedByMe = user.id in blockedIds
                )
            }
        }
    }

    fun observePendingFollowRequests(currentUserId: String): Flow<List<UserEntity>> {
        return combine(dao.observeAllFollows(), dao.observeAllUsers()) { follows, users ->
            val requesterIds = follows
                .filter { it.followingId == currentUserId && it.status == "PENDING" }
                .map { it.followerId }
                .toSet()
            users.filter { it.id in requesterIds }
        }
    }

    fun observePostsFeed(currentUserId: String): Flow<List<PostUiModel>> {
        val baseFlow = combine(
            dao.observeAllPosts(),
            dao.observeAllUsers(),
            dao.observeAllLikes(),
            dao.observeAllComments(),
            dao.observeSavedPosts(currentUserId)
        ) { posts, users, likes, comments, saved ->
            FeedPartA(posts, users, likes, comments, saved)
        }
        val extraFlow = combine(
            dao.observeAllFollows(),
            dao.observeAllPollVotes(),
            dao.observeBlockedByUser(currentUserId)
        ) { follows, votes, blocks ->
            FeedPartB(follows, votes, blocks)
        }

        return combine(baseFlow, extraFlow) { partA, partB ->
            val userMap = partA.users.associateBy { it.id }
            val blockedIds = partB.blocks.map { it.blockedId }.toSet()
            val followingIds = partB.follows
                .filter { it.followerId == currentUserId && it.status == "ACCEPTED" }
                .map { it.followingId }
                .toSet()

            partA.posts
                .filter { it.authorId !in blockedIds }
                .filter { post ->
                    val author = userMap[post.authorId]
                    val canSeePrivate = post.authorId == currentUserId ||
                        !(author?.isPrivate ?: false) ||
                        post.authorId in followingIds
                    val canSeeCircle = !post.isPrivateCircle ||
                        post.authorId == currentUserId ||
                        post.authorId in followingIds
                    canSeePrivate && canSeeCircle
                }
                .map { post ->
                    val author = userMap[post.authorId]
                    val postLikes = partA.likes.filter { it.postId == post.id }
                    val postComments = partA.comments.filter { it.postId == post.id }
                    val mediaUris = post.mediaUrisSerialized
                        .split("|")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }

                    val rawOptions = post.pollOptionsSerialized
                        .split("|")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    val postVotes = partB.votes.filter { it.postId == post.id }
                    val myVote = postVotes.firstOrNull { it.userId == currentUserId }?.optionIndex
                    val totalVotes = postVotes.size
                    val pollOptions = rawOptions.mapIndexed { idx, label ->
                        val count = postVotes.count { it.optionIndex == idx }
                        PollOptionUiModel(
                            index = idx,
                            label = label,
                            votesCount = count,
                            percentage = if (totalVotes == 0) 0f else count.toFloat() / totalVotes.toFloat(),
                            isSelectedByMe = myVote == idx
                        )
                    }

                    val hashtags = post.hashtags
                        .split(" ", ",")
                        .map { it.trim() }
                        .filter { it.startsWith("#") && it.length > 1 }

                    PostUiModel(
                        id = post.id,
                        authorId = post.authorId,
                        authorName = if (post.isAnonymousConfession) "Anonymous Spark" else (author?.fullName ?: "SPLEEX Member"),
                        authorUsername = if (post.isAnonymousConfession) "confession" else (author?.username ?: "member"),
                        authorAvatarUri = if (post.isAnonymousConfession) null else author?.avatarUri,
                        authorBadge = if (post.isAnonymousConfession) "Confession" else (author?.badgeLabel ?: ""),
                        authorVerified = !post.isAnonymousConfession && (author?.isVerified == true),
                        authorFrameStyle = if (post.isAnonymousConfession) "NONE" else (author?.frameStyle ?: "NEON_PINK"),
                        caption = post.caption,
                        hashtags = hashtags,
                        mediaUris = mediaUris,
                        postType = post.postType,
                        isAnonymousConfession = post.isAnonymousConfession,
                        isPrivateCircle = post.isPrivateCircle,
                        pollQuestion = post.pollQuestion,
                        pollOptions = pollOptions,
                        totalPollVotes = totalVotes,
                        likesCount = postLikes.size,
                        commentsCount = postComments.size,
                        isLikedByMe = postLikes.any { it.userId == currentUserId },
                        isSavedByMe = partA.saved.any { it.postId == post.id },
                        isFollowingAuthor = post.authorId in followingIds,
                        createdAt = post.createdAt
                    )
                }
        }
    }

    fun observeCommentsForPost(postId: String): Flow<List<CommentUiModel>> {
        return combine(dao.observeCommentsForPost(postId), dao.observeAllUsers()) { comments, users ->
            val userMap = users.associateBy { it.id }
            comments.map { comment ->
                val author = userMap[comment.authorId]
                CommentUiModel(
                    id = comment.id,
                    postId = comment.postId,
                    authorId = comment.authorId,
                    authorName = author?.fullName ?: "SPLEEX User",
                    authorUsername = author?.username ?: "user",
                    authorAvatarUri = author?.avatarUri,
                    authorVerified = author?.isVerified == true,
                    text = comment.text,
                    createdAt = comment.createdAt
                )
            }
        }
    }

    suspend fun createPost(
        authorId: String,
        caption: String,
        hashtagsInput: String,
        selectedImageUris: List<Uri>,
        isAnonymousConfession: Boolean,
        isPrivateCircle: Boolean,
        pollQuestion: String,
        pollOptions: List<String>
    ): Result<PostEntity> {
        val cleanCaption = caption.trim()
        val cleanPollQ = pollQuestion.trim()
        val cleanPollOpts = pollOptions.map { it.trim() }.filter { it.isNotBlank() }

        if (cleanCaption.isBlank() && selectedImageUris.isEmpty() && cleanPollQ.isBlank()) {
            return Result.failure(IllegalArgumentException("Add a caption, photo, or poll to post."))
        }
        if (cleanPollQ.isNotBlank() && cleanPollOpts.size < 2) {
            return Result.failure(IllegalArgumentException("Polls need at least 2 voting options."))
        }

        val persistedUris = selectedImageUris.map { uri ->
            mediaStorage.persistImageFromUri(uri, maxDimension = 1280)
        }

        val autoHashtags = Regex("#[A-Za-z0-9_]+")
            .findAll("$cleanCaption $hashtagsInput")
            .map { it.value }
            .distinct()
            .joinToString(" ")

        val type = when {
            isAnonymousConfession -> "CONFESSION"
            cleanPollQ.isNotBlank() -> "POLL"
            persistedUris.size > 1 -> "CAROUSEL"
            else -> "PHOTO"
        }

        val post = PostEntity(
            id = "post_${UUID.randomUUID().toString().take(8)}",
            authorId = authorId,
            caption = cleanCaption,
            hashtags = autoHashtags,
            mediaUrisSerialized = persistedUris.joinToString("|"),
            postType = type,
            isAnonymousConfession = isAnonymousConfession,
            isPrivateCircle = isPrivateCircle,
            pollQuestion = cleanPollQ,
            pollOptionsSerialized = cleanPollOpts.joinToString("|"),
            createdAt = System.currentTimeMillis()
        )
        dao.insertPost(post)
        return Result.success(post)
    }

    suspend fun toggleLike(postId: String, currentUserId: String) {
        val existing = dao.getLike(postId, currentUserId)
        if (existing != null) {
            dao.deleteLike(postId, currentUserId)
        } else {
            dao.insertLike(LikeEntity(postId = postId, userId = currentUserId))
            val posts = dao.observeAllPosts().first()
            val post = posts.firstOrNull { it.id == postId }
            if (post != null && post.authorId != currentUserId) {
                dao.insertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        recipientId = post.authorId,
                        actorId = currentUserId,
                        type = "LIKE",
                        targetId = postId,
                        message = "liked your post"
                    )
                )
            }
        }
    }

    suspend fun toggleSave(postId: String, currentUserId: String) {
        val existing = dao.getSavedPost(postId, currentUserId)
        if (existing != null) {
            dao.deleteSavedPost(postId, currentUserId)
        } else {
            dao.insertSavedPost(SavedPostEntity(postId = postId, userId = currentUserId))
        }
    }

    suspend fun addComment(postId: String, currentUserId: String, text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        dao.insertComment(
            CommentEntity(
                id = "c_${UUID.randomUUID().toString().take(8)}",
                postId = postId,
                authorId = currentUserId,
                text = clean
            )
        )
        val post = dao.observeAllPosts().first().firstOrNull { it.id == postId }
        if (post != null && post.authorId != currentUserId) {
            dao.insertNotification(
                NotificationEntity(
                    id = "notif_${UUID.randomUUID().toString().take(8)}",
                    recipientId = post.authorId,
                    actorId = currentUserId,
                    type = "COMMENT",
                    targetId = postId,
                    message = "commented: \"${clean.take(40)}\""
                )
            )
        }
    }

    suspend fun votePoll(postId: String, currentUserId: String, optionIndex: Int) {
        dao.insertPollVote(PollVoteEntity(postId = postId, userId = currentUserId, optionIndex = optionIndex))
    }

    suspend fun deletePost(postId: String) {
        dao.deletePost(postId)
    }

    suspend fun toggleFollow(currentUserId: String, targetUserId: String) {
        if (currentUserId == targetUserId) return
        val existing = dao.getFollow(currentUserId, targetUserId)
        if (existing != null) {
            dao.deleteFollow(currentUserId, targetUserId)
            return
        }
        val target = dao.getUserById(targetUserId) ?: return
        val status = if (target.isPrivate) "PENDING" else "ACCEPTED"
        dao.insertFollow(
            FollowEntity(
                followerId = currentUserId,
                followingId = targetUserId,
                status = status
            )
        )
        dao.insertNotification(
            NotificationEntity(
                id = "notif_${UUID.randomUUID().toString().take(8)}",
                recipientId = targetUserId,
                actorId = currentUserId,
                type = if (status == "PENDING") "FOLLOW_REQUEST" else "FOLLOW",
                targetId = currentUserId,
                message = if (status == "PENDING") "requested to follow you" else "started following you"
            )
        )
    }

    suspend fun respondToFollowRequest(followerId: String, targetUserId: String, accept: Boolean) {
        if (accept) {
            dao.insertFollow(FollowEntity(followerId, targetUserId, "ACCEPTED"))
        } else {
            dao.deleteFollow(followerId, targetUserId)
        }
    }

    suspend fun toggleBlockUser(currentUserId: String, targetUserId: String) {
        val existing = dao.observeBlockedByUser(currentUserId).first()
            .any { it.blockedId == targetUserId }
        if (existing) {
            dao.deleteBlock(currentUserId, targetUserId)
        } else {
            dao.insertBlock(BlockEntity(currentUserId, targetUserId))
            dao.deleteFollow(currentUserId, targetUserId)
            dao.deleteFollow(targetUserId, currentUserId)
        }
    }

    suspend fun updateProfile(
        userId: String,
        fullName: String,
        bio: String,
        website: String,
        accentHex: String,
        frameStyle: String,
        badgeLabel: String,
        isPrivate: Boolean,
        newAvatarUri: Uri?,
        newCoverUri: Uri?
    ): Result<UserEntity> {
        val current = dao.getUserById(userId)
            ?: return Result.failure(IllegalStateException("User not found"))
        val cleanName = fullName.trim()
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Name must be at least 2 characters."))
        }
        val savedAvatar = newAvatarUri?.let { mediaStorage.persistImageFromUri(it, 720) } ?: current.avatarUri
        val savedCover = newCoverUri?.let { mediaStorage.persistImageFromUri(it, 1280) } ?: current.coverUri

        val updated = current.copy(
            fullName = cleanName,
            bio = bio.trim(),
            website = website.trim(),
            accentHex = accentHex,
            frameStyle = frameStyle,
            badgeLabel = badgeLabel,
            isPrivate = isPrivate,
            avatarUri = savedAvatar,
            coverUri = savedCover
        )
        dao.updateUser(updated)
        return Result.success(updated)
    }

    // Stories
    fun observeActiveStoryGroups(currentUserId: String): Flow<List<StoryGroupUiModel>> {
        return combine(
            dao.observeActiveStories(System.currentTimeMillis()),
            dao.observeAllUsers(),
            dao.observeAllStoryViews(),
            dao.observeBlockedByUser(currentUserId)
        ) { stories, users, views, blocks ->
            val userMap = users.associateBy { it.id }
            val blockedIds = blocks.map { it.blockedId }.toSet()

            stories
                .filter { it.authorId !in blockedIds }
                .groupBy { it.authorId }
                .mapNotNull { (authorId, authorStories) ->
                    val author = userMap[authorId] ?: return@mapNotNull null
                    val items = authorStories.sortedBy { it.createdAt }.map { story ->
                        val storyViews = views.filter { it.storyId == story.id }
                        val viewerNames = storyViews.mapNotNull { view -> userMap[view.viewerId]?.fullName }
                        StoryItemUiModel(
                            id = story.id,
                            authorId = story.authorId,
                            mediaUri = story.mediaUri,
                            caption = story.caption,
                            isCloseFriends = story.isCloseFriends,
                            isViewedByMe = storyViews.any { it.viewerId == currentUserId },
                            viewsCount = storyViews.size,
                            viewerNames = viewerNames,
                            createdAt = story.createdAt,
                            expiresAt = story.expiresAt
                        )
                    }
                    StoryGroupUiModel(
                        author = author,
                        stories = items,
                        hasUnseen = items.any { !it.isViewedByMe }
                    )
                }
                .sortedWith(
                    compareByDescending<StoryGroupUiModel> { it.author.id == currentUserId }
                        .thenByDescending { it.hasUnseen }
                )
        }
    }

    suspend fun createStory(
        authorId: String,
        mediaUri: Uri,
        caption: String,
        isCloseFriends: Boolean
    ): Result<StoryEntity> {
        val persisted = mediaStorage.persistImageFromUri(mediaUri, maxDimension = 1280)
        val now = System.currentTimeMillis()
        val story = StoryEntity(
            id = "story_${UUID.randomUUID().toString().take(8)}",
            authorId = authorId,
            mediaUri = persisted,
            caption = caption.trim(),
            isCloseFriends = isCloseFriends,
            createdAt = now,
            expiresAt = now + 24L * 60L * 60L * 1000L
        )
        dao.insertStory(story)
        return Result.success(story)
    }

    suspend fun markStoryViewed(storyId: String, viewerId: String) {
        dao.insertStoryView(StoryViewEntity(storyId = storyId, viewerId = viewerId))
    }

    suspend fun reactAndReplyToStory(
        story: StoryItemUiModel,
        viewerId: String,
        emoji: String?,
        replyText: String?
    ) {
        dao.insertStoryView(
            StoryViewEntity(storyId = story.id, viewerId = viewerId, reactionEmoji = emoji)
        )
        if (story.authorId != viewerId) {
            val cleanReply = replyText?.trim().orEmpty()
            if (cleanReply.isNotBlank()) {
                val convId = startOrGetDirectChat(viewerId, story.authorId)
                sendMessage(convId, viewerId, "Story reply: $cleanReply", null)
            }
            if (!emoji.isNullOrBlank() || cleanReply.isNotBlank()) {
                dao.insertNotification(
                    NotificationEntity(
                        id = "notif_${UUID.randomUUID().toString().take(8)}",
                        recipientId = story.authorId,
                        actorId = viewerId,
                        type = "STORY_REPLY",
                        targetId = story.id,
                        message = if (cleanReply.isNotBlank()) "replied to your story: \"$cleanReply\"" else "reacted $emoji to your story"
                    )
                )
            }
        }
    }

    // Reels
    fun observeReels(currentUserId: String): Flow<List<ReelUiModel>> {
        return combine(
            dao.observeAllReels(),
            dao.observeAllUsers(),
            dao.observeAllReelLikes(),
            dao.observeAllFollows()
        ) { reels, users, likes, follows ->
            val userMap = users.associateBy { it.id }
            val followingIds = follows
                .filter { it.followerId == currentUserId && it.status == "ACCEPTED" }
                .map { it.followingId }
                .toSet()

            reels.mapNotNull { reel ->
                val author = userMap[reel.authorId] ?: return@mapNotNull null
                val reelLikes = likes.filter { it.reelId == reel.id }
                ReelUiModel(
                    id = reel.id,
                    author = author,
                    mediaUri = reel.mediaUri,
                    title = reel.title,
                    caption = reel.caption,
                    hashtags = reel.hashtags.split(" ").filter { it.startsWith("#") },
                    durationLabel = reel.durationLabel,
                    viewsCount = reel.viewsCount,
                    likesCount = reelLikes.size,
                    isLikedByMe = reelLikes.any { it.userId == currentUserId },
                    isFollowingAuthor = reel.authorId in followingIds,
                    createdAt = reel.createdAt
                )
            }
        }
    }

    suspend fun createReel(
        authorId: String,
        coverUri: Uri,
        title: String,
        caption: String,
        hashtags: String
    ): Result<ReelEntity> {
        val persisted = mediaStorage.persistImageFromUri(coverUri, maxDimension = 1280)
        val reel = ReelEntity(
            id = "reel_${UUID.randomUUID().toString().take(8)}",
            authorId = authorId,
            mediaUri = persisted,
            title = title.trim().ifBlank { "SPLEEX Moment" },
            caption = caption.trim(),
            hashtags = Regex("#[A-Za-z0-9_]+").findAll("$caption $hashtags").map { it.value }.distinct().joinToString(" "),
            durationLabel = "0:20",
            viewsCount = 1L
        )
        dao.insertReel(reel)
        return Result.success(reel)
    }

    suspend fun toggleReelLike(reelId: String, userId: String) {
        val existing = dao.getReelLike(reelId, userId)
        if (existing != null) {
            dao.deleteReelLike(reelId, userId)
        } else {
            dao.insertReelLike(ReelLikeEntity(reelId = reelId, userId = userId))
        }
    }

    suspend fun incrementReelViews(reelId: String) {
        dao.incrementReelViews(reelId)
    }

    // Messaging
    fun observeConversations(currentUserId: String): Flow<List<ConversationUiModel>> {
        return combine(
            dao.observeAllConversations(),
            dao.observeAllConversationMembers(),
            dao.observeAllUsers(),
            dao.observeAllMessages()
        ) { conversations, members, users, messages ->
            val userMap = users.associateBy { it.id }
            val myConvIds = members.filter { it.userId == currentUserId }.map { it.conversationId }.toSet()

            conversations
                .filter { it.id in myConvIds }
                .map { conv ->
                    val convMembers = members.filter { it.conversationId == conv.id }.map { it.userId }
                    val others = convMembers.filter { it != currentUserId }
                    val otherUser = others.firstOrNull()?.let { userMap[it] }
                    val unread = messages.count {
                        it.conversationId == conv.id && it.senderId != currentUserId && !it.isRead
                    }
                    val displayTitle = if (conv.isGroup) {
                        conv.title.ifBlank { "SPLEEX Circle (${convMembers.size})" }
                    } else {
                        otherUser?.fullName ?: "SPLEEX Chat"
                    }
                    ConversationUiModel(
                        id = conv.id,
                        title = displayTitle,
                        avatarUri = if (conv.isGroup) null else otherUser?.avatarUri,
                        isGroup = conv.isGroup,
                        isOnline = !conv.isGroup && (otherUser?.isOnline == true),
                        otherUserId = if (conv.isGroup) null else otherUser?.id,
                        memberCount = convMembers.size,
                        lastMessageText = conv.lastMessageText.ifBlank { "Start chatting..." },
                        lastMessageAt = conv.lastMessageAt,
                        unreadCount = unread
                    )
                }
        }
    }

    fun observeMessages(conversationId: String, currentUserId: String): Flow<List<MessageUiModel>> {
        return combine(
            dao.observeMessagesForConversation(conversationId),
            dao.observeAllUsers()
        ) { messages, users ->
            val userMap = users.associateBy { it.id }
            messages.map { msg ->
                val sender = userMap[msg.senderId]
                MessageUiModel(
                    id = msg.id,
                    conversationId = msg.conversationId,
                    senderId = msg.senderId,
                    senderName = sender?.fullName ?: "User",
                    senderAvatarUri = sender?.avatarUri,
                    isMine = msg.senderId == currentUserId,
                    text = msg.text,
                    imageUri = msg.imageUri,
                    reactionEmoji = msg.reactionEmoji,
                    isRead = msg.isRead,
                    createdAt = msg.createdAt
                )
            }
        }
    }

    suspend fun startOrGetDirectChat(currentUserId: String, otherUserId: String): String {
        val allConvs = dao.observeAllConversations().first()
        val allMembers = dao.observeAllConversationMembers().first()
        val existing = allConvs.firstOrNull { conv ->
            if (conv.isGroup) return@firstOrNull false
            val mems = allMembers.filter { it.conversationId == conv.id }.map { it.userId }.toSet()
            mems == setOf(currentUserId, otherUserId)
        }
        if (existing != null) return existing.id

        val convId = "conv_${UUID.randomUUID().toString().take(8)}"
        dao.insertConversation(
            ConversationEntity(
                id = convId,
                title = "",
                isGroup = false,
                lastMessageText = "",
                lastMessageAt = System.currentTimeMillis()
            )
        )
        dao.insertConversationMembers(
            listOf(
                ConversationMemberEntity(convId, currentUserId),
                ConversationMemberEntity(convId, otherUserId)
            )
        )
        return convId
    }

    suspend fun createGroupChat(currentUserId: String, title: String, memberIds: List<String>): String {
        val convId = "conv_${UUID.randomUUID().toString().take(8)}"
        val allIds = (memberIds + currentUserId).distinct()
        dao.insertConversation(
            ConversationEntity(
                id = convId,
                title = title.trim().ifBlank { "Private Circle" },
                isGroup = true,
                lastMessageText = "Group created",
                lastMessageAt = System.currentTimeMillis()
            )
        )
        dao.insertConversationMembers(allIds.map { ConversationMemberEntity(convId, it) })
        return convId
    }

    suspend fun sendMessage(
        conversationId: String,
        senderId: String,
        text: String,
        imageUri: Uri?
    ) {
        val cleanText = text.trim()
        if (cleanText.isBlank() && imageUri == null) return
        val persistedImg = imageUri?.let { mediaStorage.persistImageFromUri(it, 1080) }
        val now = System.currentTimeMillis()
        dao.insertMessage(
            MessageEntity(
                id = "msg_${UUID.randomUUID().toString().take(8)}",
                conversationId = conversationId,
                senderId = senderId,
                text = cleanText,
                imageUri = persistedImg,
                isRead = false,
                createdAt = now
            )
        )
        val preview = when {
            cleanText.isNotBlank() -> cleanText
            else -> "Sent a photo"
        }
        dao.updateConversationPreview(conversationId, preview, now)

        val members = dao.observeAllConversationMembers().first()
            .filter { it.conversationId == conversationId && it.userId != senderId }
        members.forEach { member ->
            dao.insertNotification(
                NotificationEntity(
                    id = "notif_${UUID.randomUUID().toString().take(8)}",
                    recipientId = member.userId,
                    actorId = senderId,
                    type = "MESSAGE",
                    targetId = conversationId,
                    message = "sent you a message: \"${preview.take(36)}\""
                )
            )
        }
    }

    suspend fun markConversationRead(conversationId: String, readerId: String) {
        dao.markConversationMessagesRead(conversationId, readerId)
    }

    suspend fun reactToMessage(messageId: String, emoji: String?) {
        dao.updateMessageReaction(messageId, emoji)
    }

    suspend fun deleteMessage(messageId: String) {
        dao.deleteMessage(messageId)
    }

    // Notifications
    fun observeNotifications(userId: String): Flow<List<NotificationUiModel>> {
        return combine(dao.observeNotificationsForUser(userId), dao.observeAllUsers()) { notifs, users ->
            val userMap = users.associateBy { it.id }
            notifs.map { notif ->
                val actor = userMap[notif.actorId]
                NotificationUiModel(
                    id = notif.id,
                    actorId = notif.actorId,
                    actorName = actor?.fullName ?: "SPLEEX",
                    actorUsername = actor?.username ?: "spleex",
                    actorAvatarUri = actor?.avatarUri,
                    type = notif.type,
                    targetId = notif.targetId,
                    message = notif.message,
                    isRead = notif.isRead,
                    createdAt = notif.createdAt
                )
            }
        }
    }

    suspend fun markAllNotificationsRead(userId: String) {
        dao.markAllNotificationsRead(userId)
    }

    private data class FeedPartA(
        val posts: List<PostEntity>,
        val users: List<UserEntity>,
        val likes: List<LikeEntity>,
        val comments: List<CommentEntity>,
        val saved: List<SavedPostEntity>
    )

    private data class FeedPartB(
        val follows: List<FollowEntity>,
        val votes: List<PollVoteEntity>,
        val blocks: List<BlockEntity>
    )
}
