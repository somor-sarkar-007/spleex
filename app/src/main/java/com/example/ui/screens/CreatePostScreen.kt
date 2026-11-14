package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private enum class CreateMode(val label: String) {
    POST("Post"),
    STORY("24h Story"),
    REEL("Short Reel")
}

@Composable
fun CreatePostScreen(
    onPublishPost: (
        caption: String,
        hashtags: String,
        imageUris: List<Uri>,
        isAnonymousConfession: Boolean,
        isPrivateCircle: Boolean,
        pollQuestion: String,
        pollOptions: List<String>
    ) -> Unit,
    onPublishStory: (imageUri: Uri, caption: String, isCloseFriends: Boolean) -> Unit,
    onPublishReel: (imageUri: Uri, title: String, caption: String, hashtags: String) -> Unit
) {
    var mode by remember { mutableStateOf(CreateMode.POST) }

    // Post state
    var caption by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    var selectedImages by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var isConfession by remember { mutableStateOf(false) }
    var isPrivateCircle by remember { mutableStateOf(false) }
    var includePoll by remember { mutableStateOf(false) }
    var pollQuestion by remember { mutableStateOf("") }
    var pollOption1 by remember { mutableStateOf("") }
    var pollOption2 by remember { mutableStateOf("") }
    var pollOption3 by remember { mutableStateOf("") }

    // Story state
    var storyUri by remember { mutableStateOf<Uri?>(null) }
    var storyCaption by remember { mutableStateOf("") }
    var isCloseFriends by remember { mutableStateOf(false) }

    // Reel state
    var reelUri by remember { mutableStateOf<Uri?>(null) }
    var reelTitle by remember { mutableStateOf("") }
    var reelCaption by remember { mutableStateOf("") }
    var reelHashtags by remember { mutableStateOf("") }

    val multiPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages = (selectedImages + uris).distinct().take(5)
        }
    }

    val singlePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            when (mode) {
                CreateMode.STORY -> storyUri = uri
                CreateMode.REEL -> reelUri = uri
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Create on SPLEEX",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Mode Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CreateMode.entries.forEach { item ->
                val selected = mode == item
                Surface(
                    onClick = { mode = item },
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("create_mode_${item.name.lowercase()}")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        when (mode) {
            CreateMode.POST -> {
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("What's on your mind?") },
                    placeholder = { Text("Share a moment, thought, or confession...") },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_post_caption_input")
                )

                OutlinedTextField(
                    value = hashtags,
                    onValueChange = { hashtags = it },
                    label = { Text("Hashtags (optional)") },
                    placeholder = { Text("#spleex #aesthetic #nightvibes") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Zero-permission Photo Picker for single or multi-image posts
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Photos (${selectedImages.size}/5)", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Upload single or carousel images",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = {
                                    multiPhotoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("pick_post_images_button")
                            ) {
                                Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Photos")
                            }
                        }

                        if (selectedImages.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(selectedImages) { uri ->
                                    Box(modifier = Modifier.size(104.dp)) {
                                        AsyncImage(
                                            model = uri,
                                            contentDescription = "Selected post photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(14.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.7f))
                                                .clickable {
                                                    selectedImages = selectedImages.filterNot { it == uri }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                contentDescription = "Remove image",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SPLEEX Special Features Toggles
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("SPLEEX Enhancements", style = MaterialTheme.typography.titleSmall)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Add Interactive Poll", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Let the community vote on your post",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(checked = includePoll, onCheckedChange = { includePoll = it })
                        }

                        if (includePoll) {
                            OutlinedTextField(
                                value = pollQuestion,
                                onValueChange = { pollQuestion = it },
                                label = { Text("Poll Question") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pollOption1,
                                onValueChange = { pollOption1 = it },
                                label = { Text("Option 1") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pollOption2,
                                onValueChange = { pollOption2 = it },
                                label = { Text("Option 2") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = pollOption3,
                                onValueChange = { pollOption3 = it },
                                label = { Text("Option 3 (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Anonymous Confession", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Hide your identity from other members",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isConfession,
                                onCheckedChange = {
                                    isConfession = it
                                    if (it) isPrivateCircle = false
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Private Circle Only", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "Visible only to followers & circle",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isPrivateCircle,
                                onCheckedChange = {
                                    isPrivateCircle = it
                                    if (it) isConfession = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onPublishPost(
                            caption,
                            hashtags,
                            selectedImages,
                            isConfession,
                            isPrivateCircle,
                            if (includePoll) pollQuestion else "",
                            if (includePoll) listOf(pollOption1, pollOption2, pollOption3) else emptyList()
                        )
                        caption = ""
                        hashtags = ""
                        selectedImages = emptyList()
                        includePoll = false
                        pollQuestion = ""
                        pollOption1 = ""
                        pollOption2 = ""
                        pollOption3 = ""
                        isConfession = false
                        isPrivateCircle = false
                    },
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("publish_post_button")
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Publish Post", style = MaterialTheme.typography.titleMedium)
                }
            }

            CreateMode.STORY -> {
                Card(
                    onClick = {
                        singlePhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("pick_story_image_card")
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (storyUri != null) {
                            AsyncImage(
                                model = storyUri,
                                contentDescription = "Selected story image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.AddAPhoto,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to pick Story photo", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Stories expire automatically in 24 hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = storyCaption,
                    onValueChange = { storyCaption = it },
                    label = { Text("Story Caption") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Close Friends Badge", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Highlight this story for your close circle",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = isCloseFriends, onCheckedChange = { isCloseFriends = it })
                }

                Button(
                    onClick = {
                        storyUri?.let { uri ->
                            onPublishStory(uri, storyCaption, isCloseFriends)
                            storyUri = null
                            storyCaption = ""
                            isCloseFriends = false
                        }
                    },
                    enabled = storyUri != null,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("publish_story_button")
                ) {
                    Text("Share 24h Story", style = MaterialTheme.typography.titleMedium)
                }
            }

            CreateMode.REEL -> {
                Card(
                    onClick = {
                        singlePhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .testTag("pick_reel_image_card")
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (reelUri != null) {
                            AsyncImage(
                                model = reelUri,
                                contentDescription = "Selected reel cover",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Outlined.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap to pick Reel visual frame", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = reelTitle,
                    onValueChange = { reelTitle = it },
                    label = { Text("Reel Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reelCaption,
                    onValueChange = { reelCaption = it },
                    label = { Text("Caption") },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reelHashtags,
                    onValueChange = { reelHashtags = it },
                    label = { Text("Hashtags") },
                    placeholder = { Text("#reels #spleex") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        reelUri?.let { uri ->
                            onPublishReel(uri, reelTitle, reelCaption, reelHashtags)
                            reelUri = null
                            reelTitle = ""
                            reelCaption = ""
                            reelHashtags = ""
                        }
                    },
                    enabled = reelUri != null,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("publish_reel_button")
                ) {
                    Text("Publish Reel", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
