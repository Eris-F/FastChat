package com.fastchat.ui.screens

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fastchat.data.models.Message
import com.fastchat.data.models.MessageStatus
import com.fastchat.data.models.MessageType
import com.fastchat.ui.theme.*
import com.fastchat.utils.DateTimeUtils
import com.fastchat.viewmodels.AuthViewModel
import com.fastchat.viewmodels.ChatViewModel
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    authViewModel: AuthViewModel,
    chatViewModel: ChatViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val messages by chatViewModel.messages.collectAsState()
    val currentChat by chatViewModel.currentChat.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messageText by remember { mutableStateOf("") }
    var otherUserName by remember { mutableStateOf("") }
    var otherUserPhoto by remember { mutableStateOf<String?>(null) }
    var otherUserOnline by remember { mutableStateOf(false) }
    var otherUserLastSeen by remember { mutableStateOf(0L) }
    var recipientKey by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
                val imageBytes = outputStream.toByteArray()

                currentUser?.let { user ->
                    chatViewModel.sendImageMessage(
                        chatId,
                        user.uid,
                        user.username,
                        imageBytes,
                        recipientKey
                    )
                }
            }
        }
    }

    LaunchedEffect(chatId, currentUser) {
        currentUser?.let { user ->
            chatViewModel.loadMessagesForChat(chatId, user.encryptionKey)
            chatViewModel.markMessagesAsRead(chatId, user.uid)

            // Load chat details
            val chat = chatViewModel.chats.value.find { it.id == chatId }
            chat?.let {
                chatViewModel.setCurrentChat(it)
                val otherUserId = it.getOtherParticipantId(user.uid)
                otherUserName = it.getOtherParticipantName(user.uid)
                otherUserPhoto = it.getOtherParticipantPhoto(user.uid)

                // Get other user details
                otherUserId?.let { userId ->
                    val otherUser = chatViewModel.getUser(userId)
                    otherUser?.let { u ->
                        otherUserOnline = u.isOnline
                        otherUserLastSeen = u.lastSeen
                        recipientKey = u.encryptionKey
                    }
                }
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = otherUserName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (otherUserOnline) {
                                "Online"
                            } else if (otherUserLastSeen > 0) {
                                DateTimeUtils.formatLastSeen(otherUserLastSeen)
                            } else {
                                "Offline"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (otherUserOnline) {
                                Color(0xFF4CAF50)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View profile") },
                            onClick = {
                                showMenu = false
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { imagePickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.Image, contentDescription = "Send image")
                    }

                    IconButton(onClick = {
                        // TODO: Voice recording
                    }) {
                        Icon(Icons.Default.Mic, contentDescription = "Voice message")
                    }

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = {
                            messageText = it
                            currentUser?.let { user ->
                                chatViewModel.setTypingStatus(chatId, user.uid, it.isNotEmpty())
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        placeholder = { Text("Type a message...") },
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4
                    )

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                currentUser?.let { user ->
                                    chatViewModel.sendMessage(
                                        chatId,
                                        user.uid,
                                        user.username,
                                        messageText,
                                        recipientKey
                                    )
                                    messageText = ""
                                    chatViewModel.setTypingStatus(chatId, user.uid, false)
                                }
                            }
                        },
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            items(messages) { message ->
                currentUser?.let { user ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == user.uid,
                        onDeleteMessage = { forEveryone ->
                            chatViewModel.deleteMessage(message.id, forEveryone)
                        },
                        onDownloadMedia = {
                            scope.launch {
                                message.mediaUrl?.let { url ->
                                    chatViewModel.downloadAndDecryptMedia(url, user.encryptionKey)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    onDeleteMessage: (Boolean) -> Unit,
    onDownloadMedia: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (message.deletedForEveryone) {
            Text(
                text = "This message was deleted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(8.dp)
            )
        } else if (message.deleted && isCurrentUser) {
            Text(
                text = "You deleted this message",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(8.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                            bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isCurrentUser) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable { showMenu = true }
                    .padding(12.dp)
            ) {
                Column {
                    when (message.type) {
                        MessageType.TEXT -> {
                            Text(
                                text = message.encryptedContent,
                                color = if (isCurrentUser) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        MessageType.IMAGE -> {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Image message",
                                tint = if (isCurrentUser) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = "Image",
                                color = if (isCurrentUser) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        MessageType.VOICE -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Voice message",
                                    tint = if (isCurrentUser) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Voice message",
                                    color = if (isCurrentUser) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = DateTimeUtils.formatMessageTime(message.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCurrentUser) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )

                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            when (message.status) {
                                MessageStatus.SENDING -> {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = "Sending",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    )
                                }
                                MessageStatus.SENT -> {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = "Sent",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    )
                                }
                                MessageStatus.DELIVERED -> {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Delivered",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    )
                                }
                                MessageStatus.READ -> {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Read",
                                        modifier = Modifier.size(14.dp),
                                        tint = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (isCurrentUser) {
                        DropdownMenuItem(
                            text = { Text("Delete for me") },
                            onClick = {
                                onDeleteMessage(false)
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete for everyone") },
                            onClick = {
                                onDeleteMessage(true)
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
