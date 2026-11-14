package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.CommentUiModel
import com.example.data.model.PostUiModel
import com.example.ui.theme.SpleexPeachAccent
import com.example.ui.theme.SpleexPinkGlow
import com.example.ui.theme.SpleexPinkPrimary
import com.example.ui.theme.SpleexSuccess
import com.example.ui.theme.SpleexVioletAccent

@Composable
fun SpleexWordmark(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(SpleexPinkPrimary, SpleexVioletAccent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = "SPLEEX",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SpleexAvatar(
    imageUri: String?,
    name: String,
    size: Dp = 44.dp,
    frameStyle: String = "NEON_PINK",
    isOnline: Boolean = false,
    showRing: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val ringColors = when (frameStyle) {
        "VIOLET_AURA" -> listOf(SpleexVioletAccent, SpleexPinkPrimary)
        "GOLD_SPARK" -> listOf(Color(0xFFFFD166), SpleexPinkPrimary)
        "SOFT_ROSE" -> listOf(SpleexPeachAccent, Color(0xFFFF8FAB))
        "NONE" -> listOf(MaterialTheme.colorScheme.outline, MaterialTheme.colorScheme.outline)
        else -> listOf(SpleexPinkGlow, SpleexVioletAccent)
    }
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "S" }

    Box(
        modifier = modifier
            .size(size)
            .let { base ->
                if (onClick != null) base.clickable(onClick = onClick) else base
            },
        contentAlignment = Alignment.Center
    ) {
        val borderModifier = if (showRing || frameStyle != "NONE") {
            Modifier.border(
                width = if (showRing) 2.5.dp else 1.5.dp,
                brush = Brush.linearGradient(ringColors),
                shape = CircleShape
            )
        } else Modifier

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(borderModifier)
                .padding(if (showRing) 3.dp else 1.5.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!imageUri.isNullOrBlank()) {
                AsyncImage(
                    model = imageUri,
                    contentDescription = "$name profile picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size * 0.27f).coerceIn(10.dp, 14.dp))
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(SpleexSuccess)
            )
        }
    }
}

@Composable
fun PostCard(
    post: PostUiModel,
    currentUserId: String,
    onAuthorClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCommentClick: (PostUiModel) -> Unit,
    onFollowClick: (String) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onHashtagClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val likeScale by animateFloatAsState(
        targetValue = if (post.isLikedByMe) 1.18f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "likeScale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 7.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(vertical = 14.dp)) {
            // Post Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpleexAvatar(
                    imageUri = post.authorAvatarUri,
                    name = post.authorName,
                    size = 42.dp,
                    frameStyle = post.authorFrameStyle,
                    onClick = {
                        if (!post.isAnonymousConfession) onAuthorClick(post.authorId)
                    }
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(enabled = !post.isAnonymousConfession) {
                            onAuthorClick(post.authorId)
                        }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.authorVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "Verified creator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (post.isAnonymousConfession || post.isPrivateCircle) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (post.isAnonymousConfession) "Confession" else "Circle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "@${post.authorUsername} • ${formatRelativeTime(post.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!post.isAnonymousConfession && post.authorId != currentUserId && !post.isFollowingAuthor) {
                    TextButton(
                        onClick = { onFollowClick(post.authorId) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(
                            text = "Follow",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.MoreHoriz,
                            contentDescription = "Post options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (post.isSavedByMe) "Remove from Saved" else "Save Post") },
                            leadingIcon = {
                                Icon(
                                    if (post.isSavedByMe) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                showMenu = false
                                onSaveClick(post.id)
                            }
                        )
                        if (post.authorId == currentUserId) {
                            DropdownMenuItem(
                                text = { Text("Delete Post", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick(post.id)
                                }
                            )
                        }
                    }
                }
            }

            // Confession styling or normal caption
            if (post.caption.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                if (post.isAnonymousConfession && post.mediaUris.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "“${post.caption}”",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                } else {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )
                }
            }

            // Hashtags
            if (post.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    post.hashtags.take(4).forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { onHashtagClick(tag) }
                        )
                    }
                }
            }

            // Media Gallery / Carousel
            if (post.mediaUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                if (post.mediaUris.size == 1) {
                    AsyncImage(
                        model = post.mediaUris.first(),
                        contentDescription = "Post image by ${post.authorName}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .height(290.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    val pagerState = rememberPagerState(pageCount = { post.mediaUris.size })
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                                .clip(RoundedCornerShape(18.dp))
                        ) { page ->
                            AsyncImage(
                                model = post.mediaUris[page],
                                contentDescription = "Post photo ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1}/${post.mediaUris.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Poll Section
            if (post.pollQuestion.isNotBlank() && post.pollOptions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = post.pollQuestion,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    post.pollOptions.forEach { option ->
                        val pctText = "${(option.percentage * 100).toInt()}%"
                        Surface(
                            onClick = { onVotePoll(post.id, option.index) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (option.isSelectedByMe) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                1.dp,
                                if (option.isSelectedByMe) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(option.percentage.coerceIn(0f, 1f))
                                        .matchParentSize()
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = option.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (option.isSelectedByMe) FontWeight.Bold else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = pctText,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "${post.totalPollVotes} votes",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Action Bar
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onLikeClick(post.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (post.isLikedByMe) "Unlike post" else "Like post",
                        tint = if (post.isLikedByMe) SpleexPinkGlow else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(22.dp)
                            .scale(likeScale)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likesCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (post.isLikedByMe) SpleexPinkGlow else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onCommentClick(post) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out ${post.authorName}'s post on SPLEEX: ${post.caption}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share post",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                IconButton(
                    onClick = { onSaveClick(post.id) },
                    modifier = Modifier.testTag("save_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isSavedByMe) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (post.isSavedByMe) "Saved" else "Save post",
                        tint = if (post.isSavedByMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: PostUiModel,
    comments: List<CommentUiModel>,
    onDismiss: () -> Unit,
    onAddComment: (String) -> Unit,
    onAuthorClick: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 340.dp, max = 560.dp)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Comments (${comments.size})",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (comments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No comments yet. Start the conversation!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(comments, key = { it.id }) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            SpleexAvatar(
                                imageUri = comment.authorAvatarUri,
                                name = comment.authorName,
                                size = 34.dp,
                                onClick = {
                                    onDismiss()
                                    onAuthorClick(comment.authorId)
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = comment.authorName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = formatRelativeTime(comment.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text("Add a comment...") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input")
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledIconButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            onAddComment(draft)
                            draft = ""
                        }
                    },
                    enabled = draft.isNotBlank(),
                    modifier = Modifier.testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Post comment"
                    )
                }
            }
        }
    }
}

fun formatRelativeTime(timestampMillis: Long): String {
    val diff = (System.currentTimeMillis() - timestampMillis).coerceAtLeast(0L)
    val seconds = diff / 1000L
    val minutes = seconds / 60L
    val hours = minutes / 60L
    val days = hours / 24L
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        days < 7 -> "${days}d"
        else -> "${days / 7}w"
    }
}
