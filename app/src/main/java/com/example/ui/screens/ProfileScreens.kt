package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.UserEntity
import com.example.data.model.PostUiModel
import com.example.data.model.ReelUiModel
import com.example.data.model.UserSummaryUiModel
import com.example.ui.components.PostCard
import com.example.ui.components.SpleexAvatar
import com.example.ui.theme.SpleexPinkPrimary
import com.example.ui.theme.SpleexVioletAccent

private enum class ProfileTab(val label: String) {
    POSTS("Posts"),
    REELS("Reels"),
    SAVED("Saved"),
    BADGES("Badges")
}

@Composable
fun UserProfileScreen(
    currentUserId: String,
    profileSummary: UserSummaryUiModel?,
    allPosts: List<PostUiModel>,
    allReels: List<ReelUiModel>,
    pendingFollowRequests: List<UserEntity>,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onFollowClick: (String) -> Unit,
    onMessageClick: (String) -> Unit,
    onRespondFollowRequest: (String, Boolean) -> Unit,
    onBlockUser: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCommentClick: (PostUiModel) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onHashtagClick: (String) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenReels: () -> Unit
) {
    if (showBackButton) {
        BackHandler(onBack = onBackClick)
    }

    val context = LocalContext.current
    val summary = profileSummary ?: return
    val user = summary.user
    val isSelf = user.id == currentUserId
    var selectedTab by remember { mutableStateOf(ProfileTab.POSTS) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val userPosts = remember(allPosts, user.id) {
        allPosts.filter { it.authorId == user.id && !it.isAnonymousConfession }
    }
    val userReels = remember(allReels, user.id) {
        allReels.filter { it.author.id == user.id }
    }
    val savedPosts = remember(allPosts) {
        allPosts.filter { it.isSavedByMe }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Cover + Avatar Header
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(164.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    SpleexPinkPrimary.copy(alpha = 0.45f),
                                    SpleexVioletAccent.copy(alpha = 0.35f)
                                )
                            )
                        )
                ) {
                    if (!user.coverUri.isNullOrBlank()) {
                        AsyncImage(
                            model = user.coverUri,
                            contentDescription = "${user.fullName} cover image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent)
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showBackButton) {
                            IconButton(
                                onClick = onBackClick,
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.45f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        } else {
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Connect with ${user.fullName} (@${user.username}) on SPLEEX!"
                                        )
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = Color.Black.copy(alpha = 0.45f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share profile")
                            }

                            if (isSelf) {
                                IconButton(
                                    onClick = onOpenSettingsClick,
                                    colors = IconButtonDefaults.iconButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.45f),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.testTag("profile_settings_button")
                                ) {
                                    Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                                }
                            } else {
                                Box {
                                    IconButton(
                                        onClick = { showMoreMenu = true },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = Color.Black.copy(alpha = 0.45f),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.Filled.MoreVert, contentDescription = "Profile options")
                                    }
                                    DropdownMenu(
                                        expanded = showMoreMenu,
                                        onDismissRequest = { showMoreMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text(if (summary.isBlockedByMe) "Unblock User" else "Block User")
                                            },
                                            leadingIcon = { Icon(Icons.Outlined.Block, contentDescription = null) },
                                            onClick = {
                                                showMoreMenu = false
                                                onBlockUser(user.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Avatar overlapping bottom of cover
                Box(
                    modifier = Modifier
                        .padding(start = 18.dp, top = 114.dp)
                ) {
                    SpleexAvatar(
                        imageUri = user.avatarUri,
                        name = user.fullName,
                        size = 92.dp,
                        frameStyle = user.frameStyle,
                        isOnline = user.isOnline
                    )
                }
            }
        }

        // User Info + Stats + Actions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    if (user.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    if (user.isPrivate) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Private account",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "@${user.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (user.badgeLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = user.badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (user.bio.isNotBlank()) {
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (user.website.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = user.website,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Stats Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStatColumn("Posts", summary.postsCount.toString())
                        ProfileStatColumn("Followers", summary.followersCount.toString())
                        ProfileStatColumn("Following", summary.followingCount.toString())
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Primary Actions
                if (isSelf) {
                    Button(
                        onClick = onEditProfileClick,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Outlined.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit & Customize Profile")
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val followLabel = when (summary.followStatus) {
                            "ACCEPTED" -> "Following"
                            "PENDING" -> "Requested"
                            else -> "Follow"
                        }
                        if (summary.followStatus == null) {
                            Button(
                                onClick = { onFollowClick(user.id) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("profile_follow_button")
                            ) {
                                Text(followLabel)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onFollowClick(user.id) },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("profile_follow_button")
                            ) {
                                Text(followLabel)
                            }
                        }
                        FilledTonalButton(
                            onClick = { onMessageClick(user.id) },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("profile_message_button")
                        ) {
                            Icon(Icons.Outlined.ChatBubbleOutline, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Message")
                        }
                    }
                }
            }
        }

        // Pending Follow Requests (Own Private Profile)
        if (isSelf && pendingFollowRequests.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Follow Requests (${pendingFollowRequests.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        pendingFollowRequests.forEach { requester ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpleexAvatar(
                                    imageUri = requester.avatarUri,
                                    name = requester.fullName,
                                    size = 38.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(requester.fullName, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "@${requester.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { onRespondFollowRequest(requester.id, false) }) {
                                    Text("Decline")
                                }
                                Button(onClick = { onRespondFollowRequest(requester.id, true) }) {
                                    Text("Accept")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tabs Row
        item {
            val visibleTabs = if (isSelf) {
                ProfileTab.entries
            } else {
                listOf(ProfileTab.POSTS, ProfileTab.REELS, ProfileTab.BADGES)
            }
            TabRow(
                selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0),
                containerColor = MaterialTheme.colorScheme.background
            ) {
                visibleTabs.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }
        }

        // Tab Content
        when (selectedTab) {
            ProfileTab.POSTS -> {
                if (userPosts.isEmpty()) {
                    item {
                        EmptyProfileState(
                            icon = Icons.Outlined.PhotoLibrary,
                            title = "No posts yet",
                            subtitle = if (isSelf) "Share photos or polls from the Create tab." else "This creator hasn't posted yet."
                        )
                    }
                } else {
                    items(userPosts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            currentUserId = currentUserId,
                            onAuthorClick = {},
                            onLikeClick = onLikeClick,
                            onSaveClick = onSaveClick,
                            onCommentClick = onCommentClick,
                            onFollowClick = onFollowClick,
                            onVotePoll = onVotePoll,
                            onHashtagClick = onHashtagClick,
                            onDeleteClick = onDeletePost
                        )
                    }
                }
            }

            ProfileTab.REELS -> {
                if (userReels.isEmpty()) {
                    item {
                        EmptyProfileState(
                            icon = Icons.Outlined.VideoLibrary,
                            title = "No reels yet",
                            subtitle = "Short creator reels will appear here."
                        )
                    }
                } else {
                    items(userReels, key = { it.id }) { reel ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable(onClick = onOpenReels),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = reel.mediaUri,
                                    contentDescription = reel.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(reel.title, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        reel.caption,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "${reel.viewsCount} views • ${reel.likesCount} likes",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Icon(Icons.Filled.PlayArrow, contentDescription = "Watch")
                            }
                        }
                    }
                }
            }

            ProfileTab.SAVED -> {
                if (savedPosts.isEmpty()) {
                    item {
                        EmptyProfileState(
                            icon = Icons.Outlined.BookmarkBorder,
                            title = "No saved posts",
                            subtitle = "Bookmark posts on the feed to keep them private here."
                        )
                    }
                } else {
                    items(savedPosts, key = { it.id }) { post ->
                        PostCard(
                            post = post,
                            currentUserId = currentUserId,
                            onAuthorClick = {},
                            onLikeClick = onLikeClick,
                            onSaveClick = onSaveClick,
                            onCommentClick = onCommentClick,
                            onFollowClick = onFollowClick,
                            onVotePoll = onVotePoll,
                            onHashtagClick = onHashtagClick,
                            onDeleteClick = onDeletePost
                        )
                    }
                }
            }

            ProfileTab.BADGES -> {
                item {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AchievementCard(
                            title = "Early Spark",
                            description = "Joined the SPLEEX creator platform.",
                            unlocked = true
                        )
                        AchievementCard(
                            title = "Community Voice",
                            description = "Published ${summary.postsCount} post(s) to the social feed.",
                            unlocked = summary.postsCount >= 1
                        )
                        AchievementCard(
                            title = "Circle Builder",
                            description = "Reached ${summary.followersCount} follower(s) on SPLEEX.",
                            unlocked = summary.followersCount >= 2
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileStatColumn(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyProfileState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AchievementCard(title: String, description: String, unlocked: Boolean) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Filled.EmojiEvents else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    user: UserEntity,
    onBackClick: () -> Unit,
    onSaveProfile: (
        fullName: String,
        bio: String,
        website: String,
        accentHex: String,
        frameStyle: String,
        badgeLabel: String,
        isPrivate: Boolean,
        newAvatarUri: Uri?,
        newCoverUri: Uri?
    ) -> Unit
) {
    BackHandler(onBack = onBackClick)

    var fullName by remember(user) { mutableStateOf(user.fullName) }
    var bio by remember(user) { mutableStateOf(user.bio) }
    var website by remember(user) { mutableStateOf(user.website) }
    var badgeLabel by remember(user) { mutableStateOf(user.badgeLabel) }
    var frameStyle by remember(user) { mutableStateOf(user.frameStyle) }
    var isPrivate by remember(user) { mutableStateOf(user.isPrivate) }
    var newAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var newCoverUri by remember { mutableStateOf<Uri?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) newAvatarUri = uri }

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) newCoverUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            onSaveProfile(
                                fullName,
                                bio,
                                website,
                                user.accentHex,
                                frameStyle,
                                badgeLabel,
                                isPrivate,
                                newAvatarUri,
                                newCoverUri
                            )
                        },
                        modifier = Modifier.testTag("save_profile_button")
                    ) {
                        Text("Save", style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Cover Picker
            Card(
                onClick = {
                    coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .testTag("pick_cover_button")
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val coverModel: Any? = newCoverUri ?: user.coverUri
                    if (coverModel != null) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = "Cover image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = 0.6f)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Change Cover Photo", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }

            // Avatar Picker
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                    .testTag("pick_avatar_button")
            ) {
                SpleexAvatar(
                    imageUri = newAvatarUri?.toString() ?: user.avatarUri,
                    name = fullName,
                    size = 76.dp,
                    frameStyle = frameStyle
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text("Change Profile Photo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pick a crisp portrait from your gallery",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = website,
                onValueChange = { website = it },
                label = { Text("Website Link") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = badgeLabel,
                onValueChange = { badgeLabel = it },
                label = { Text("Profile Badge") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Profile Frame Style", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "NEON_PINK" to "Neon Pink",
                    "VIOLET_AURA" to "Violet",
                    "GOLD_SPARK" to "Gold",
                    "SOFT_ROSE" to "Rose"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = frameStyle == key,
                        onClick = { frameStyle = key },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Private Account", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Approve follow requests before people see your posts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    user: UserEntity?,
    isDarkMode: Boolean,
    onToggleDarkMode: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Appearance", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Premium Dark + Soft Pink", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Switch off for SPLEEX Light Mode",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(checked = isDarkMode, onCheckedChange = onToggleDarkMode)
                    }
                }
            }

            Card(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Account Info", style = MaterialTheme.typography.titleMedium)
                    Text("Signed in as @${user?.username.orEmpty()}", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        user?.email.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Persistence: Local SQLite Room Database + On-Device Media Storage",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogoutClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }
}
