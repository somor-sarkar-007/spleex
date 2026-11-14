package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.UserEntity
import com.example.data.model.PostUiModel
import com.example.data.model.StoryGroupUiModel
import com.example.ui.components.PostCard
import com.example.ui.components.SpleexAvatar
import com.example.ui.components.SpleexWordmark

private enum class FeedFilter { FOR_YOU, FOLLOWING }

@Composable
fun HomeFeedScreen(
    currentUser: UserEntity?,
    posts: List<PostUiModel>,
    storyGroups: List<StoryGroupUiModel>,
    unreadNotificationsCount: Int,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenReels: () -> Unit,
    onAddStoryClick: () -> Unit,
    onStoryGroupClick: (String) -> Unit,
    onAuthorClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCommentClick: (PostUiModel) -> Unit,
    onFollowClick: (String) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onHashtagClick: (String) -> Unit,
    onDeletePost: (String) -> Unit
) {
    var selectedFeed by remember { mutableStateOf(FeedFilter.FOR_YOU) }

    val visiblePosts = remember(posts, selectedFeed, currentUser) {
        if (selectedFeed == FeedFilter.FOR_YOU) {
            posts
        } else {
            posts.filter { it.isFollowingAuthor || it.authorId == currentUser?.id }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Top Brand Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpleexWordmark()
                Spacer(modifier = Modifier.weight(1f))

                Surface(
                    onClick = onOpenReels,
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.testTag("home_reels_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayCircle,
                            contentDescription = "Watch Reels",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Reels",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier.testTag("theme_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Toggle theme",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onOpenNotifications,
                    modifier = Modifier.testTag("notifications_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (unreadNotificationsCount > 0) {
                                Badge { Text(unreadNotificationsCount.coerceAtMost(99).toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Stories Strip
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable(onClick = onAddStoryClick)
                            .testTag("add_story_tile")
                    ) {
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SpleexAvatar(
                                imageUri = currentUser?.avatarUri,
                                name = currentUser?.fullName ?: "You",
                                size = 60.dp,
                                frameStyle = "NONE"
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add story",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Your Story",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                items(storyGroups, key = { it.author.id }) { group ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(68.dp)
                            .clickable { onStoryGroupClick(group.author.id) }
                            .testTag("story_ring_${group.author.id}")
                    ) {
                        SpleexAvatar(
                            imageUri = group.author.avatarUri,
                            name = group.author.fullName,
                            size = 64.dp,
                            showRing = group.hasUnseen,
                            frameStyle = if (group.hasUnseen) "NEON_PINK" else "NONE"
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = group.author.username,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // For You / Following Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FeedFilterPill(
                    label = "For You",
                    selected = selectedFeed == FeedFilter.FOR_YOU,
                    onClick = { selectedFeed = FeedFilter.FOR_YOU },
                    modifier = Modifier.testTag("feed_tab_for_you")
                )
                FeedFilterPill(
                    label = "Following",
                    selected = selectedFeed == FeedFilter.FOLLOWING,
                    onClick = { selectedFeed = FeedFilter.FOLLOWING },
                    modifier = Modifier.testTag("feed_tab_following")
                )
            }
        }

        if (visiblePosts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.DynamicFeed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Your feed is quiet",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Follow creators in Explore or share your first photo post to light up SPLEEX.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(visiblePosts, key = { it.id }) { post ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PostCard(
                        post = post,
                        currentUserId = currentUser?.id.orEmpty(),
                        onAuthorClick = onAuthorClick,
                        onLikeClick = onLikeClick,
                        onSaveClick = onSaveClick,
                        onCommentClick = onCommentClick,
                        onFollowClick = onFollowClick,
                        onVotePoll = onVotePoll,
                        onHashtagClick = onHashtagClick,
                        onDeleteClick = onDeletePost,
                        modifier = Modifier.widthIn(max = 620.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedFilterPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.height(38.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
