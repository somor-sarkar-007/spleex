package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.PostUiModel
import com.example.data.model.UserSummaryUiModel
import com.example.ui.components.PostCard
import com.example.ui.components.SpleexAvatar
import com.example.ui.theme.SpleexPinkPrimary
import com.example.ui.theme.SpleexVioletAccent

@Composable
fun ExploreScreen(
    currentUserId: String,
    initialQuery: String,
    posts: List<PostUiModel>,
    userSummaries: List<UserSummaryUiModel>,
    recentSearches: List<String>,
    onQuerySubmit: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onAuthorClick: (String) -> Unit,
    onFollowClick: (String) -> Unit,
    onLikeClick: (String) -> Unit,
    onSaveClick: (String) -> Unit,
    onCommentClick: (PostUiModel) -> Unit,
    onVotePoll: (String, Int) -> Unit,
    onDeletePost: (String) -> Unit,
    onOpenReels: () -> Unit
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }

    val trendingTags = remember(posts) {
        posts.flatMap { it.hashtags }
            .groupingBy { it.lowercase() }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .ifEmpty { listOf("#spleex", "#nightphotography", "#minimal", "#reels", "#creativity") }
    }

    val creators = remember(userSummaries, currentUserId, query) {
        userSummaries.filter { it.user.id != currentUserId && !it.isBlockedByMe }
            .filter {
                query.isBlank() ||
                    it.user.fullName.contains(query, ignoreCase = true) ||
                    it.user.username.contains(query, ignoreCase = true)
            }
    }

    val filteredPosts = remember(posts, query) {
        if (query.isBlank()) {
            posts.sortedByDescending { it.likesCount + it.commentsCount }
        } else {
            posts.filter {
                it.caption.contains(query, ignoreCase = true) ||
                    it.authorName.contains(query, ignoreCase = true) ||
                    it.authorUsername.contains(query, ignoreCase = true) ||
                    it.hashtags.any { tag -> tag.contains(query, ignoreCase = true) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Explore & Search",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search creators, posts, or #hashtags") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotBlank()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input")
                )
            }
        }

        // Recent Searches
        if (recentSearches.isNotEmpty() && query.isBlank()) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent", style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = onClearRecentSearches) {
                            Text("Clear")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentSearches) { item ->
                            AssistChip(
                                onClick = { query = item },
                                label = { Text(item) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.History,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // Trending Hashtags
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text(
                    text = "Trending Hashtags",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trendingTags) { tag ->
                        FilterChip(
                            selected = query.equals(tag, ignoreCase = true),
                            onClick = {
                                query = if (query.equals(tag, ignoreCase = true)) "" else tag
                                if (query.isNotBlank()) onQuerySubmit(tag)
                            },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }

        // Reels Highlight Banner
        if (query.isBlank()) {
            item {
                Surface(
                    onClick = onOpenReels,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        SpleexPinkPrimary.copy(alpha = 0.28f),
                                        SpleexVioletAccent.copy(alpha = 0.24f)
                                    )
                                )
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SlowMotionVideo,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Popular Short Reels", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Swipe vertical creator clips & trending drops",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                    }
                }
            }
        }

        // Recommended Creators
        if (creators.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        text = if (query.isBlank()) "Suggested Creators" else "Matching Creators",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(creators, key = { it.user.id }) { summary ->
                            Card(
                                modifier = Modifier
                                    .width(164.dp)
                                    .clickable {
                                        if (query.isNotBlank()) onQuerySubmit(query)
                                        onAuthorClick(summary.user.id)
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    SpleexAvatar(
                                        imageUri = summary.user.avatarUri,
                                        name = summary.user.fullName,
                                        size = 56.dp,
                                        frameStyle = summary.user.frameStyle,
                                        isOnline = summary.user.isOnline
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = summary.user.fullName,
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "@${summary.user.username}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${summary.followersCount} followers",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val btnLabel = when (summary.followStatus) {
                                        "ACCEPTED" -> "Following"
                                        "PENDING" -> "Requested"
                                        else -> "Follow"
                                    }
                                    if (summary.followStatus == null) {
                                        Button(
                                            onClick = { onFollowClick(summary.user.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                        ) {
                                            Text(btnLabel, style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { onFollowClick(summary.user.id) },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                        ) {
                                            Text(btnLabel, style = MaterialTheme.typography.labelMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Photo Discovery Grid when not searching
        val mediaPosts = filteredPosts.filter { it.mediaUris.isNotEmpty() }
        if (mediaPosts.isNotEmpty() && query.isBlank()) {
            item {
                Text(
                    text = "Visual Discovery",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            item {
                val rows = mediaPosts.take(6).chunked(3)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            rowItems.forEach { post ->
                                AsyncImage(
                                    model = post.mediaUris.first(),
                                    contentDescription = post.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onCommentClick(post) }
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // Trending / Search Results Posts
        item {
            Text(
                text = if (query.isBlank()) "Trending Posts" else "Matching Posts (${filteredPosts.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        items(filteredPosts, key = { it.id }) { post ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PostCard(
                    post = post,
                    currentUserId = currentUserId,
                    onAuthorClick = onAuthorClick,
                    onLikeClick = onLikeClick,
                    onSaveClick = onSaveClick,
                    onCommentClick = onCommentClick,
                    onFollowClick = onFollowClick,
                    onVotePoll = onVotePoll,
                    onHashtagClick = { tag -> query = tag },
                    onDeleteClick = onDeletePost,
                    modifier = Modifier.widthIn(max = 620.dp)
                )
            }
        }
    }
}
