package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ReelUiModel
import com.example.data.model.StoryGroupUiModel
import com.example.data.model.StoryItemUiModel
import com.example.ui.components.SpleexAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.SpleexPinkGlow
import kotlinx.coroutines.delay

@Composable
fun StoryViewerScreen(
    storyGroups: List<StoryGroupUiModel>,
    initialAuthorId: String,
    currentUserId: String,
    onMarkViewed: (String) -> Unit,
    onReactAndReply: (StoryItemUiModel, String?, String?) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val initialGroupIndex = remember(storyGroups, initialAuthorId) {
        storyGroups.indexOfFirst { it.author.id == initialAuthorId }.coerceAtLeast(0)
    }
    var groupIdx by remember { mutableIntStateOf(initialGroupIndex) }
    var storyIdx by remember { mutableIntStateOf(0) }
    var replyDraft by remember { mutableStateOf("") }
    var showViewersSheet by remember { mutableStateOf(false) }

    val currentGroup = storyGroups.getOrNull(groupIdx)
    val currentStory = currentGroup?.stories?.getOrNull(storyIdx)

    LaunchedEffect(currentStory?.id) {
        if (currentStory != null) {
            onMarkViewed(currentStory.id)
        }
    }

    if (currentGroup == null || currentStory == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    // Auto-advance every 6 seconds when not typing
    LaunchedEffect(groupIdx, storyIdx, replyDraft.isNotEmpty(), showViewersSheet) {
        if (replyDraft.isEmpty() && !showViewersSheet) {
            delay(6000L)
            if (storyIdx + 1 < currentGroup.stories.size) {
                storyIdx++
            } else if (groupIdx + 1 < storyGroups.size) {
                groupIdx++
                storyIdx = 0
            } else {
                onClose()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(groupIdx, storyIdx) {
                detectTapGestures { offset ->
                    val width = size.width
                    if (offset.x < width * 0.35f) {
                        if (storyIdx > 0) {
                            storyIdx--
                        } else if (groupIdx > 0) {
                            groupIdx--
                            storyIdx = (storyGroups[groupIdx].stories.size - 1).coerceAtLeast(0)
                        }
                    } else {
                        if (storyIdx + 1 < currentGroup.stories.size) {
                            storyIdx++
                        } else if (groupIdx + 1 < storyGroups.size) {
                            groupIdx++
                            storyIdx = 0
                        } else {
                            onClose()
                        }
                    }
                }
            }
    ) {
        AsyncImage(
            model = currentStory.mediaUri,
            contentDescription = "Story by ${currentGroup.author.fullName}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Top & Bottom Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.65f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.78f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Progress + Author
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    currentGroup.stories.forEachIndexed { idx, _ ->
                        val progress by animateFloatAsState(
                            targetValue = when {
                                idx < storyIdx -> 1f
                                idx == storyIdx -> 1f
                                else -> 0f
                            },
                            animationSpec = tween(durationMillis = if (idx == storyIdx) 6000 else 150, easing = LinearEasing),
                            label = "storyProgress"
                        )
                        LinearProgressIndicator(
                            progress = { if (idx < storyIdx) 1f else if (idx > storyIdx) 0f else progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpleexAvatar(
                        imageUri = currentGroup.author.avatarUri,
                        name = currentGroup.author.fullName,
                        size = 38.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentGroup.author.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                color = Color.White
                            )
                            if (currentStory.isCloseFriends) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF10B981)
                                ) {
                                    Text(
                                        text = "Close Friends",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = formatRelativeTime(currentStory.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.testTag("close_story_button")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
                    }
                }
            }

            // Bottom Caption + Reactions / View Count
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentStory.caption.isNotBlank()) {
                    Text(
                        text = currentStory.caption,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }

                if (currentStory.authorId == currentUserId) {
                    Surface(
                        onClick = { showViewersSheet = true },
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${currentStory.viewsCount} view(s)",
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("❤️", "🔥", "😍", "👏").forEach { emoji ->
                            Surface(
                                onClick = { onReactAndReply(currentStory, emoji, null) },
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = emoji,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = replyDraft,
                            onValueChange = { replyDraft = it },
                            placeholder = { Text("Send story reply...", color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledIconButton(
                            onClick = {
                                if (replyDraft.isNotBlank()) {
                                    onReactAndReply(currentStory, null, replyDraft)
                                    replyDraft = ""
                                }
                            },
                            enabled = replyDraft.isNotBlank()
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send story reply")
                        }
                    }
                }
            }
        }

        if (showViewersSheet) {
            AlertDialog(
                onDismissRequest = { showViewersSheet = false },
                title = { Text("Story Viewers (${currentStory.viewsCount})") },
                text = {
                    if (currentStory.viewerNames.isEmpty()) {
                        Text("No viewers yet.")
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            currentStory.viewerNames.forEach { name ->
                                Text("• $name")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showViewersSheet = false }) { Text("Close") }
                }
            )
        }
    }
}

@Composable
fun ReelsScreen(
    currentUserId: String,
    reels: List<ReelUiModel>,
    onBackClick: () -> Unit,
    onLikeReel: (String) -> Unit,
    onFollowCreator: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onReelViewed: (String) -> Unit,
    onShowToast: (String) -> Unit
) {
    BackHandler(onBack = onBackClick)
    val context = LocalContext.current

    if (reels.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("No reels yet", style = MaterialTheme.typography.titleLarge, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onBackClick) { Text("Back") }
            }
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { reels.size })

    LaunchedEffect(pagerState.currentPage) {
        reels.getOrNull(pagerState.currentPage)?.let { onReelViewed(it.id) }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val reel = reels[page]
            var isPlaying by remember { mutableStateOf(true) }
            var playbackProgress by remember { mutableFloatStateOf(0.15f) }

            LaunchedEffect(isPlaying, page) {
                while (isPlaying) {
                    delay(120L)
                    playbackProgress = if (playbackProgress >= 1f) 0f else playbackProgress + 0.015f
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isPlaying = !isPlaying }
            ) {
                AsyncImage(
                    model = reel.mediaUri,
                    contentDescription = reel.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                if (!isPlaying) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                contentDescription = "Paused",
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                }

                // Right vertical actions
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(end = 14.dp, bottom = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { onLikeReel(reel.id) }) {
                            Icon(
                                imageVector = if (reel.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like reel",
                                tint = if (reel.isLikedByMe) SpleexPinkGlow else Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Text("${reel.likesCount}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        IconButton(onClick = { onShowToast("Views: ${reel.viewsCount}") }) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = "Views",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text("${reel.viewsCount}", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Watch \"${reel.title}\" by @${reel.author.username} on SPLEEX!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Reel"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share reel",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Left Creator Info
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(start = 16.dp, end = 80.dp, bottom = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SpleexAvatar(
                            imageUri = reel.author.avatarUri,
                            name = reel.author.fullName,
                            size = 40.dp,
                            onClick = { onAuthorClick(reel.author.id) }
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "@${reel.author.username}",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            modifier = Modifier.clickable { onAuthorClick(reel.author.id) }
                        )
                        if (reel.author.id != currentUserId) {
                            Spacer(modifier = Modifier.width(10.dp))
                            OutlinedButton(
                                onClick = { onFollowCreator(reel.author.id) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    text = if (reel.isFollowingAuthor) "Following" else "Follow",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    Text(reel.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text(reel.caption, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                    if (reel.hashtags.isNotEmpty()) {
                        Text(
                            text = reel.hashtags.joinToString(" "),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { playbackProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick, modifier = Modifier.testTag("reels_back_button")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("SPLEEX Reels", style = MaterialTheme.typography.titleLarge, color = Color.White)
        }
    }
}
