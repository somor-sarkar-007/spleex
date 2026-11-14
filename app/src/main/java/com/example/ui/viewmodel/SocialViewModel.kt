package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.UserEntity
import com.example.data.model.*
import com.example.data.repository.SpleexRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SocialViewModel(
    private val repo: SpleexRepository,
    val currentUserId: String
) : ViewModel() {

    val currentUser: StateFlow<UserEntity?> = repo.observeCurrentUser(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allPosts: StateFlow<List<PostUiModel>> = repo.observePostsFeed(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userSummaries: StateFlow<List<UserSummaryUiModel>> = repo.observeUserSummaries(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingFollowRequests: StateFlow<List<UserEntity>> = repo.observePendingFollowRequests(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val storyGroups: StateFlow<List<StoryGroupUiModel>> = repo.observeActiveStoryGroups(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reels: StateFlow<List<ReelUiModel>> = repo.observeReels(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversations: StateFlow<List<ConversationUiModel>> = repo.observeConversations(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationUiModel>> = repo.observeNotifications(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentSearches: StateFlow<List<String>> = repo.recentSearchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun consumeToast() {
        _toastMessage.value = null
    }

    fun showToast(message: String) {
        _toastMessage.value = message
    }

    fun observeComments(postId: String): Flow<List<CommentUiModel>> = repo.observeCommentsForPost(postId)

    fun observeMessages(conversationId: String): Flow<List<MessageUiModel>> =
        repo.observeMessages(conversationId, currentUserId)

    fun toggleLike(postId: String) {
        viewModelScope.launch { repo.toggleLike(postId, currentUserId) }
    }

    fun toggleSave(postId: String) {
        viewModelScope.launch {
            repo.toggleSave(postId, currentUserId)
            _toastMessage.value = "Updated saved posts"
        }
    }

    fun addComment(postId: String, text: String) {
        viewModelScope.launch { repo.addComment(postId, currentUserId, text) }
    }

    fun votePoll(postId: String, optionIndex: Int) {
        viewModelScope.launch { repo.votePoll(postId, currentUserId, optionIndex) }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repo.deletePost(postId)
            _toastMessage.value = "Post removed"
        }
    }

    fun toggleFollow(targetUserId: String) {
        viewModelScope.launch { repo.toggleFollow(currentUserId, targetUserId) }
    }

    fun respondToFollowRequest(followerId: String, accept: Boolean) {
        viewModelScope.launch {
            repo.respondToFollowRequest(followerId, currentUserId, accept)
            _toastMessage.value = if (accept) "Follow request accepted" else "Request declined"
        }
    }

    fun toggleBlockUser(targetUserId: String) {
        viewModelScope.launch {
            repo.toggleBlockUser(currentUserId, targetUserId)
            _toastMessage.value = "Updated block list"
        }
    }

    fun createPost(
        caption: String,
        hashtags: String,
        imageUris: List<Uri>,
        isAnonymousConfession: Boolean,
        isPrivateCircle: Boolean,
        pollQuestion: String,
        pollOptions: List<String>,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val result = repo.createPost(
                authorId = currentUserId,
                caption = caption,
                hashtagsInput = hashtags,
                selectedImageUris = imageUris,
                isAnonymousConfession = isAnonymousConfession,
                isPrivateCircle = isPrivateCircle,
                pollQuestion = pollQuestion,
                pollOptions = pollOptions
            )
            if (result.isSuccess) {
                _toastMessage.value = "Shared to SPLEEX!"
                onSuccess()
            } else {
                _toastMessage.value = result.exceptionOrNull()?.message ?: "Could not publish post"
            }
        }
    }

    fun createStory(
        imageUri: Uri,
        caption: String,
        isCloseFriends: Boolean,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repo.createStory(currentUserId, imageUri, caption, isCloseFriends)
            _toastMessage.value = "Story live for 24h!"
            onSuccess()
        }
    }

    fun createReel(
        imageUri: Uri,
        title: String,
        caption: String,
        hashtags: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            repo.createReel(currentUserId, imageUri, title, caption, hashtags)
            _toastMessage.value = "Reel published!"
            onSuccess()
        }
    }

    fun updateProfile(
        fullName: String,
        bio: String,
        website: String,
        accentHex: String,
        frameStyle: String,
        badgeLabel: String,
        isPrivate: Boolean,
        newAvatarUri: Uri?,
        newCoverUri: Uri?,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val res = repo.updateProfile(
                userId = currentUserId,
                fullName = fullName,
                bio = bio,
                website = website,
                accentHex = accentHex,
                frameStyle = frameStyle,
                badgeLabel = badgeLabel,
                isPrivate = isPrivate,
                newAvatarUri = newAvatarUri,
                newCoverUri = newCoverUri
            )
            if (res.isSuccess) {
                _toastMessage.value = "Profile updated"
                onSuccess()
            } else {
                _toastMessage.value = res.exceptionOrNull()?.message ?: "Update failed"
            }
        }
    }

    fun markStoryViewed(storyId: String) {
        viewModelScope.launch { repo.markStoryViewed(storyId, currentUserId) }
    }

    fun reactAndReplyToStory(story: StoryItemUiModel, emoji: String?, replyText: String?) {
        viewModelScope.launch {
            repo.reactAndReplyToStory(story, currentUserId, emoji, replyText)
            if (!replyText.isNullOrBlank()) {
                _toastMessage.value = "Reply sent"
            }
        }
    }

    fun toggleReelLike(reelId: String) {
        viewModelScope.launch { repo.toggleReelLike(reelId, currentUserId) }
    }

    fun incrementReelViews(reelId: String) {
        viewModelScope.launch { repo.incrementReelViews(reelId) }
    }

    fun openDirectChat(otherUserId: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val convId = repo.startOrGetDirectChat(currentUserId, otherUserId)
            onReady(convId)
        }
    }

    fun createGroupChat(title: String, memberIds: List<String>, onReady: (String) -> Unit) {
        viewModelScope.launch {
            val convId = repo.createGroupChat(currentUserId, title, memberIds)
            onReady(convId)
        }
    }

    fun sendMessage(conversationId: String, text: String, imageUri: Uri?) {
        viewModelScope.launch { repo.sendMessage(conversationId, currentUserId, text, imageUri) }
    }

    fun markConversationRead(conversationId: String) {
        viewModelScope.launch { repo.markConversationRead(conversationId, currentUserId) }
    }

    fun reactToMessage(messageId: String, emoji: String?) {
        viewModelScope.launch { repo.reactToMessage(messageId, emoji) }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch { repo.deleteMessage(messageId) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { repo.markAllNotificationsRead(currentUserId) }
    }

    fun addRecentSearch(query: String) {
        viewModelScope.launch { repo.addRecentSearch(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { repo.clearRecentSearches() }
    }
}
