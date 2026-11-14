package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.PostUiModel
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.SpleexPinkGlow
import com.example.ui.theme.SpleexPinkPrimary
import com.example.ui.theme.SpleexTheme
import com.example.ui.theme.SpleexVioletAccent
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.SocialViewModel

object SpleexRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val CREATE = "create"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"
    const val USER_PROFILE = "user_profile/{userId}"
    const val EDIT_PROFILE = "edit_profile"
    const val SETTINGS = "settings"
    const val STORY_VIEWER = "story_viewer/{authorId}"
    const val REELS = "reels"
    const val CHAT_DETAIL = "chat_detail/{conversationId}"
    const val NOTIFICATIONS = "notifications"

    fun userProfile(userId: String) = "user_profile/$userId"
    fun storyViewer(authorId: String) = "story_viewer/$authorId"
    fun chatDetail(conversationId: String) = "chat_detail/$conversationId"
}

private data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = AppContainer.getInstance(applicationContext)

        setContent {
            val authViewModel: AuthViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AuthViewModel(appContainer) as T
                    }
                }
            )
            val currentUserId by authViewModel.currentUserId.collectAsStateWithLifecycle()
            val isDarkMode by authViewModel.isDarkMode.collectAsStateWithLifecycle()
            val hasSeenOnboarding by authViewModel.hasSeenOnboarding.collectAsStateWithLifecycle()
            val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()

            SpleexTheme(darkTheme = isDarkMode) {
                val activeUid = currentUserId
                if (activeUid == null) {
                    AuthFlowScreen(
                        uiState = authUiState,
                        hasSeenOnboarding = hasSeenOnboarding,
                        onDismissOnboarding = { authViewModel.markOnboardingSeen() },
                        onLogin = { id, pw -> authViewModel.login(id, pw) },
                        onRegister = { name, user, email, pw, confirm, avatar ->
                            authViewModel.register(name, user, email, pw, confirm, avatar)
                        },
                        onResetPassword = { email, newPw, confirmPw, onDone ->
                            authViewModel.resetPassword(email, newPw, confirmPw, onDone)
                        },
                        onDemoLogin = { authViewModel.loginDemoAccount() },
                        onClearMessages = { authViewModel.clearMessages() }
                    )
                } else {
                    key(activeUid) {
                        SpleexMainApp(
                            container = appContainer,
                            currentUserId = activeUid,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = { authViewModel.toggleDarkMode(it) },
                            onLogout = { authViewModel.logout() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpleexMainApp(
    container: AppContainer,
    currentUserId: String,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    val socialViewModel: SocialViewModel = viewModel(
        key = currentUserId,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SocialViewModel(container.repository, currentUserId) as T
            }
        }
    )

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentUser by socialViewModel.currentUser.collectAsStateWithLifecycle()
    val allPosts by socialViewModel.allPosts.collectAsStateWithLifecycle()
    val userSummaries by socialViewModel.userSummaries.collectAsStateWithLifecycle()
    val pendingFollowRequests by socialViewModel.pendingFollowRequests.collectAsStateWithLifecycle()
    val storyGroups by socialViewModel.storyGroups.collectAsStateWithLifecycle()
    val reels by socialViewModel.reels.collectAsStateWithLifecycle()
    val conversations by socialViewModel.conversations.collectAsStateWithLifecycle()
    val notifications by socialViewModel.notifications.collectAsStateWithLifecycle()
    val recentSearches by socialViewModel.recentSearches.collectAsStateWithLifecycle()
    val toastMessage by socialViewModel.toastMessage.collectAsStateWithLifecycle()

    var activeCommentPost by remember { mutableStateOf<PostUiModel?>(null) }
    var exploreInitialQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            socialViewModel.consumeToast()
        }
    }

    val unreadNotifCount = remember(notifications) { notifications.count { !it.isRead } }
    val unreadMessagesCount = remember(conversations) { conversations.sumOf { it.unreadCount } }

    val primaryRoutes = remember {
        setOf(
            SpleexRoutes.HOME,
            SpleexRoutes.EXPLORE,
            SpleexRoutes.CREATE,
            SpleexRoutes.MESSAGES,
            SpleexRoutes.PROFILE
        )
    }
    val showPrimaryNav = currentRoute in primaryRoutes

    val navItems = remember {
        listOf(
            BottomNavItem(SpleexRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
            BottomNavItem(SpleexRoutes.EXPLORE, "Explore", Icons.Filled.Explore, Icons.Outlined.Explore),
            BottomNavItem(SpleexRoutes.CREATE, "Create", Icons.Filled.Add, Icons.Outlined.Add),
            BottomNavItem(SpleexRoutes.MESSAGES, "Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
            BottomNavItem(SpleexRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.PersonOutline)
        )
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openProfile: (String) -> Unit = { targetId ->
        if (targetId == currentUserId) {
            navigateToTab(SpleexRoutes.PROFILE)
        } else {
            navController.navigate(SpleexRoutes.userProfile(targetId))
        }
    }

    val openHashtagInExplore: (String) -> Unit = { tag ->
        exploreInitialQuery = tag
        navigateToTab(SpleexRoutes.EXPLORE)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 680.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (showPrimaryNav && !useNavRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            val isCreate = item.route == SpleexRoutes.CREATE
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    if (isCreate) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(SpleexPinkPrimary, SpleexPinkGlow, SpleexVioletAccent)
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = item.label,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    } else if (item.route == SpleexRoutes.MESSAGES && unreadMessagesCount > 0) {
                                        BadgedBox(
                                            badge = { Badge { Text(unreadMessagesCount.coerceAtMost(99).toString()) } }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.label
                                        )
                                    }
                                },
                                label = {
                                    if (!isCreate) {
                                        Text(item.label, style = MaterialTheme.typography.labelSmall)
                                    }
                                },
                                modifier = Modifier.testTag("nav_${item.route}")
                            )
                        }
                    }
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showPrimaryNav && useNavRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = SpleexRoutes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(SpleexRoutes.HOME) {
                        HomeFeedScreen(
                            currentUser = currentUser,
                            posts = allPosts,
                            storyGroups = storyGroups,
                            unreadNotificationsCount = unreadNotifCount,
                            isDarkMode = isDarkMode,
                            onToggleTheme = { onToggleDarkMode(!isDarkMode) },
                            onOpenNotifications = { navController.navigate(SpleexRoutes.NOTIFICATIONS) },
                            onOpenReels = { navController.navigate(SpleexRoutes.REELS) },
                            onAddStoryClick = { navigateToTab(SpleexRoutes.CREATE) },
                            onStoryGroupClick = { authorId ->
                                navController.navigate(SpleexRoutes.storyViewer(authorId))
                            },
                            onAuthorClick = openProfile,
                            onLikeClick = { socialViewModel.toggleLike(it) },
                            onSaveClick = { socialViewModel.toggleSave(it) },
                            onCommentClick = { activeCommentPost = it },
                            onFollowClick = { socialViewModel.toggleFollow(it) },
                            onVotePoll = { postId, opt -> socialViewModel.votePoll(postId, opt) },
                            onHashtagClick = openHashtagInExplore,
                            onDeletePost = { socialViewModel.deletePost(it) }
                        )
                    }

                    composable(SpleexRoutes.EXPLORE) {
                        ExploreScreen(
                            currentUserId = currentUserId,
                            initialQuery = exploreInitialQuery,
                            posts = allPosts,
                            userSummaries = userSummaries,
                            recentSearches = recentSearches,
                            onQuerySubmit = { socialViewModel.addRecentSearch(it) },
                            onClearRecentSearches = { socialViewModel.clearRecentSearches() },
                            onAuthorClick = openProfile,
                            onFollowClick = { socialViewModel.toggleFollow(it) },
                            onLikeClick = { socialViewModel.toggleLike(it) },
                            onSaveClick = { socialViewModel.toggleSave(it) },
                            onCommentClick = { activeCommentPost = it },
                            onVotePoll = { postId, opt -> socialViewModel.votePoll(postId, opt) },
                            onDeletePost = { socialViewModel.deletePost(it) },
                            onOpenReels = { navController.navigate(SpleexRoutes.REELS) }
                        )
                    }

                    composable(SpleexRoutes.CREATE) {
                        CreatePostScreen(
                            onPublishPost = { caption, tags, uris, isConfession, isCircle, pollQ, pollOpts ->
                                socialViewModel.createPost(
                                    caption = caption,
                                    hashtags = tags,
                                    imageUris = uris,
                                    isAnonymousConfession = isConfession,
                                    isPrivateCircle = isCircle,
                                    pollQuestion = pollQ,
                                    pollOptions = pollOpts,
                                    onSuccess = { navigateToTab(SpleexRoutes.HOME) }
                                )
                            },
                            onPublishStory = { uri, cap, closeFriends ->
                                socialViewModel.createStory(uri, cap, closeFriends) {
                                    navigateToTab(SpleexRoutes.HOME)
                                }
                            },
                            onPublishReel = { uri, title, cap, tags ->
                                socialViewModel.createReel(uri, title, cap, tags) {
                                    navController.navigate(SpleexRoutes.REELS)
                                }
                            }
                        )
                    }

                    composable(SpleexRoutes.MESSAGES) {
                        MessagesScreen(
                            currentUserId = currentUserId,
                            conversations = conversations,
                            userSummaries = userSummaries,
                            onOpenConversation = { convId ->
                                navController.navigate(SpleexRoutes.chatDetail(convId))
                            },
                            onStartDirectChat = { otherId ->
                                socialViewModel.openDirectChat(otherId) { convId ->
                                    navController.navigate(SpleexRoutes.chatDetail(convId))
                                }
                            },
                            onCreateGroupChat = { title, memberIds ->
                                socialViewModel.createGroupChat(title, memberIds) { convId ->
                                    navController.navigate(SpleexRoutes.chatDetail(convId))
                                }
                            }
                        )
                    }

                    composable(SpleexRoutes.PROFILE) {
                        val ownSummary = userSummaries.firstOrNull { it.user.id == currentUserId }
                        UserProfileScreen(
                            currentUserId = currentUserId,
                            profileSummary = ownSummary,
                            allPosts = allPosts,
                            allReels = reels,
                            pendingFollowRequests = pendingFollowRequests,
                            showBackButton = false,
                            onBackClick = {},
                            onEditProfileClick = { navController.navigate(SpleexRoutes.EDIT_PROFILE) },
                            onOpenSettingsClick = { navController.navigate(SpleexRoutes.SETTINGS) },
                            onFollowClick = {},
                            onMessageClick = {},
                            onRespondFollowRequest = { reqId, accept ->
                                socialViewModel.respondToFollowRequest(reqId, accept)
                            },
                            onBlockUser = {},
                            onLikeClick = { socialViewModel.toggleLike(it) },
                            onSaveClick = { socialViewModel.toggleSave(it) },
                            onCommentClick = { activeCommentPost = it },
                            onVotePoll = { postId, opt -> socialViewModel.votePoll(postId, opt) },
                            onHashtagClick = openHashtagInExplore,
                            onDeletePost = { socialViewModel.deletePost(it) },
                            onOpenReels = { navController.navigate(SpleexRoutes.REELS) }
                        )
                    }

                    composable(
                        route = SpleexRoutes.USER_PROFILE,
                        arguments = listOf(navArgument("userId") { type = NavType.StringType })
                    ) { entry ->
                        val targetId = entry.arguments?.getString("userId").orEmpty()
                        val targetSummary = userSummaries.firstOrNull { it.user.id == targetId }
                        UserProfileScreen(
                            currentUserId = currentUserId,
                            profileSummary = targetSummary,
                            allPosts = allPosts,
                            allReels = reels,
                            pendingFollowRequests = emptyList(),
                            showBackButton = true,
                            onBackClick = { navController.popBackStack() },
                            onEditProfileClick = {},
                            onOpenSettingsClick = {},
                            onFollowClick = { socialViewModel.toggleFollow(it) },
                            onMessageClick = { otherId ->
                                socialViewModel.openDirectChat(otherId) { convId ->
                                    navController.navigate(SpleexRoutes.chatDetail(convId))
                                }
                            },
                            onRespondFollowRequest = { _, _ -> },
                            onBlockUser = {
                                socialViewModel.toggleBlockUser(it)
                                navController.popBackStack()
                            },
                            onLikeClick = { socialViewModel.toggleLike(it) },
                            onSaveClick = { socialViewModel.toggleSave(it) },
                            onCommentClick = { activeCommentPost = it },
                            onVotePoll = { postId, opt -> socialViewModel.votePoll(postId, opt) },
                            onHashtagClick = openHashtagInExplore,
                            onDeletePost = { socialViewModel.deletePost(it) },
                            onOpenReels = { navController.navigate(SpleexRoutes.REELS) }
                        )
                    }

                    composable(SpleexRoutes.EDIT_PROFILE) {
                        val user = currentUser
                        if (user != null) {
                            EditProfileScreen(
                                user = user,
                                onBackClick = { navController.popBackStack() },
                                onSaveProfile = { name, bio, web, accent, frame, badge, isPriv, avatar, cover ->
                                    socialViewModel.updateProfile(
                                        fullName = name,
                                        bio = bio,
                                        website = web,
                                        accentHex = accent,
                                        frameStyle = frame,
                                        badgeLabel = badge,
                                        isPrivate = isPriv,
                                        newAvatarUri = avatar,
                                        newCoverUri = cover,
                                        onSuccess = { navController.popBackStack() }
                                    )
                                }
                            )
                        }
                    }

                    composable(SpleexRoutes.SETTINGS) {
                        SettingsScreen(
                            user = currentUser,
                            isDarkMode = isDarkMode,
                            onToggleDarkMode = onToggleDarkMode,
                            onBackClick = { navController.popBackStack() },
                            onLogoutClick = onLogout
                        )
                    }

                    composable(
                        route = SpleexRoutes.STORY_VIEWER,
                        arguments = listOf(navArgument("authorId") { type = NavType.StringType })
                    ) { entry ->
                        val authorId = entry.arguments?.getString("authorId").orEmpty()
                        StoryViewerScreen(
                            storyGroups = storyGroups,
                            initialAuthorId = authorId,
                            currentUserId = currentUserId,
                            onMarkViewed = { socialViewModel.markStoryViewed(it) },
                            onReactAndReply = { story, emoji, reply ->
                                socialViewModel.reactAndReplyToStory(story, emoji, reply)
                            },
                            onClose = { navController.popBackStack() }
                        )
                    }

                    composable(SpleexRoutes.REELS) {
                        ReelsScreen(
                            currentUserId = currentUserId,
                            reels = reels,
                            onBackClick = { navController.popBackStack() },
                            onLikeReel = { socialViewModel.toggleReelLike(it) },
                            onFollowCreator = { socialViewModel.toggleFollow(it) },
                            onAuthorClick = openProfile,
                            onReelViewed = { socialViewModel.incrementReelViews(it) },
                            onShowToast = { socialViewModel.showToast(it) }
                        )
                    }

                    composable(
                        route = SpleexRoutes.CHAT_DETAIL,
                        arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
                    ) { entry ->
                        val convId = entry.arguments?.getString("conversationId").orEmpty()
                        val conversation = conversations.firstOrNull { it.id == convId }
                        val messages by socialViewModel.observeMessages(convId)
                            .collectAsStateWithLifecycle(initialValue = emptyList())

                        ChatDetailScreen(
                            conversation = conversation,
                            messages = messages,
                            onBackClick = { navController.popBackStack() },
                            onMarkRead = { socialViewModel.markConversationRead(convId) },
                            onSendMessage = { text, imgUri ->
                                socialViewModel.sendMessage(convId, text, imgUri)
                            },
                            onReactMessage = { msgId, emoji ->
                                socialViewModel.reactToMessage(msgId, emoji)
                            },
                            onDeleteMessage = { socialViewModel.deleteMessage(it) }
                        )
                    }

                    composable(SpleexRoutes.NOTIFICATIONS) {
                        NotificationsScreen(
                            notifications = notifications,
                            onBackClick = { navController.popBackStack() },
                            onMarkAllRead = { socialViewModel.markAllNotificationsRead() },
                            onActorClick = openProfile
                        )
                    }
                }
            }
        }
    }

    activeCommentPost?.let { post ->
        val comments by socialViewModel.observeComments(post.id)
            .collectAsStateWithLifecycle(initialValue = emptyList())
        CommentsBottomSheet(
            post = post,
            comments = comments,
            onDismiss = { activeCommentPost = null },
            onAddComment = { text -> socialViewModel.addComment(post.id, text) },
            onAuthorClick = openProfile
        )
    }
}
