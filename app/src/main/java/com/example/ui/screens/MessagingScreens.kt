package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.ConversationUiModel
import com.example.data.model.MessageUiModel
import com.example.data.model.NotificationUiModel
import com.example.data.model.UserSummaryUiModel
import com.example.ui.components.SpleexAvatar
import com.example.ui.components.formatRelativeTime

@Composable
fun MessagesScreen(
    currentUserId: String,
    conversations: List<ConversationUiModel>,
    userSummaries: List<UserSummaryUiModel>,
    onOpenConversation: (String) -> Unit,
    onStartDirectChat: (String) -> Unit,
    onCreateGroupChat: (String, List<String>) -> Unit
) {
    var showNewChatDialog by remember { mutableStateOf(false) }
    var isGroupMode by remember { mutableStateOf(false) }
    var groupTitle by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf<Set<String>>(emptySet()) }

    val availableUsers = remember(userSummaries, currentUserId) {
        userSummaries.filter { it.user.id != currentUserId && !it.isBlockedByMe }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Messages",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            FilledTonalButton(
                onClick = { showNewChatDialog = true },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("new_chat_button")
            ) {
                Icon(Icons.Outlined.EditNote, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Chat")
            }
        }

        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("No conversations yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tap New Chat to message a creator or start a group.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations, key = { it.id }) { conv ->
                    Card(
                        onClick = { onOpenConversation(conv.id) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conversation_${conv.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (conv.isGroup) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Filled.Groups,
                                        contentDescription = conv.title,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                SpleexAvatar(
                                    imageUri = conv.avatarUri,
                                    name = conv.title,
                                    size = 50.dp,
                                    isOnline = conv.isOnline
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = conv.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = formatRelativeTime(conv.lastMessageAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = conv.lastMessageText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (conv.unreadCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Badge { Text(conv.unreadCount.toString()) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChatDialog) {
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            title = { Text(if (isGroupMode) "New Group Chat" else "Start Conversation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !isGroupMode,
                            onClick = { isGroupMode = false },
                            label = { Text("Direct") }
                        )
                        FilterChip(
                            selected = isGroupMode,
                            onClick = { isGroupMode = true },
                            label = { Text("Group Circle") }
                        )
                    }
                    if (isGroupMode) {
                        OutlinedTextField(
                            value = groupTitle,
                            onValueChange = { groupTitle = it },
                            label = { Text("Group Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    availableUsers.forEach { summary ->
                        val isSelected = summary.user.id in selectedMembers
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isGroupMode) {
                                        selectedMembers = if (isSelected) {
                                            selectedMembers - summary.user.id
                                        } else {
                                            selectedMembers + summary.user.id
                                        }
                                    } else {
                                        showNewChatDialog = false
                                        onStartDirectChat(summary.user.id)
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpleexAvatar(
                                imageUri = summary.user.avatarUri,
                                name = summary.user.fullName,
                                size = 36.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.user.fullName, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "@${summary.user.username}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isGroupMode) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isGroupMode) {
                    Button(
                        onClick = {
                            if (selectedMembers.isNotEmpty()) {
                                showNewChatDialog = false
                                onCreateGroupChat(groupTitle, selectedMembers.toList())
                                groupTitle = ""
                                selectedMembers = emptySet()
                            }
                        },
                        enabled = selectedMembers.isNotEmpty()
                    ) {
                        Text("Create Group")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewChatDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: ConversationUiModel?,
    messages: List<MessageUiModel>,
    onBackClick: () -> Unit,
    onMarkRead: () -> Unit,
    onSendMessage: (String, Uri?) -> Unit,
    onReactMessage: (String, String?) -> Unit,
    onDeleteMessage: (String) -> Unit
) {
    BackHandler(onBack = onBackClick)

    LaunchedEffect(conversation?.id, messages.size) {
        onMarkRead()
    }

    var draft by remember { mutableStateOf("") }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedMessageForActions by remember { mutableStateOf<MessageUiModel?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) pendingImageUri = uri }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (conversation != null) {
                            SpleexAvatar(
                                imageUri = conversation.avatarUri,
                                name = conversation.title,
                                size = 36.dp,
                                isOnline = conversation.isOnline
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Column {
                            Text(
                                text = conversation?.title ?: "Chat",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when {
                                    conversation?.isGroup == true -> "${conversation.memberCount} members"
                                    conversation?.isOnline == true -> "Active now"
                                    else -> "Offline"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                if (pendingImageUri != null) {
                    Box(modifier = Modifier.padding(bottom = 8.dp)) {
                        AsyncImage(
                            model = pendingImageUri,
                            contentDescription = "Selected attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        IconButton(
                            onClick = { pendingImageUri = null },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Filled.Cancel, contentDescription = "Remove attachment", tint = Color.White)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.testTag("chat_attach_photo_button")
                    ) {
                        Icon(
                            Icons.Outlined.AddPhotoAlternate,
                            contentDescription = "Attach photo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = { Text("Message...") },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            onSendMessage(draft, pendingImageUri)
                            draft = ""
                            pendingImageUri = null
                        },
                        enabled = draft.isNotBlank() || pendingImageUri != null,
                        modifier = Modifier.testTag("chat_send_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (msg.isMine) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        onClick = { selectedMessageForActions = msg },
                        shape = RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (msg.isMine) 20.dp else 6.dp,
                            bottomEnd = if (msg.isMine) 6.dp else 20.dp
                        ),
                        color = if (msg.isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 290.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (!msg.isMine && conversation?.isGroup == true) {
                                Text(
                                    text = msg.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                            }
                            if (!msg.imageUri.isNullOrBlank()) {
                                AsyncImage(
                                    model = msg.imageUri,
                                    contentDescription = "Shared photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(170.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                if (msg.text.isNotBlank()) Spacer(modifier = Modifier.height(6.dp))
                            }
                            if (msg.text.isNotBlank()) {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (msg.isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        if (!msg.reactionEmoji.isNullOrBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = msg.reactionEmoji,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = if (msg.isMine) {
                                "${formatRelativeTime(msg.createdAt)} • ${if (msg.isRead) "Seen" else "Sent"}"
                            } else {
                                formatRelativeTime(msg.createdAt)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    selectedMessageForActions?.let { msg ->
        AlertDialog(
            onDismissRequest = { selectedMessageForActions = null },
            title = { Text("Message Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("React with emoji", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf("❤️", "🔥", "😂", "👍").forEach { emoji ->
                            Surface(
                                onClick = {
                                    onReactMessage(msg.id, emoji)
                                    selectedMessageForActions = null
                                },
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(emoji, modifier = Modifier.padding(10.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (msg.isMine) {
                    TextButton(
                        onClick = {
                            onDeleteMessage(msg.id)
                            selectedMessageForActions = null
                        }
                    ) {
                        Text("Delete Message", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedMessageForActions = null }) { Text("Close") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<NotificationUiModel>,
    onBackClick: () -> Unit,
    onMarkAllRead: () -> Unit,
    onActorClick: (String) -> Unit
) {
    BackHandler(onBack = onBackClick)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (notifications.any { !it.isRead }) {
                        TextButton(onClick = onMarkAllRead) {
                            Text("Mark all read")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (notifications.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("You are all caught up.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    Card(
                        onClick = { onActorClick(notif.actorId) },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (notif.isRead) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpleexAvatar(
                                imageUri = notif.actorAvatarUri,
                                name = notif.actorName,
                                size = 42.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${notif.actorName} ${notif.message}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = formatRelativeTime(notif.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!notif.isRead) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
