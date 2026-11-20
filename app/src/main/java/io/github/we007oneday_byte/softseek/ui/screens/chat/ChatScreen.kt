package io.github.we007oneday_byte.softseek.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.AttachmentType
import io.github.we007oneday_byte.softseek.domain.model.ChatMessage
import io.github.we007oneday_byte.softseek.domain.model.Conversation
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment
import io.github.we007oneday_byte.softseek.domain.model.MessageRole
import io.github.we007oneday_byte.softseek.domain.model.MessageStatus
import io.github.we007oneday_byte.softseek.domain.model.ReasoningEffort
import io.github.we007oneday_byte.softseek.ui.components.common.MarkdownText
import io.github.we007oneday_byte.softseek.ui.components.common.SettingItem
import io.github.we007oneday_byte.softseek.ui.theme.LocalSoftSeekUiSettings
import io.github.we007oneday_byte.softseek.ui.theme.SoftSeekButton
import io.github.we007oneday_byte.softseek.ui.theme.SoftSeekIconButton
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareBubble
import io.github.we007oneday_byte.softseek.ui.theme.softSeekColors
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    onOpenSettings: () -> Unit,
    onOpenCall: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var renameTarget by remember { mutableStateOf<Conversation?>(null) }
    var deleteTarget by remember { mutableStateOf<Conversation?>(null) }
    var editTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var previewImage by remember { mutableStateOf<String?>(null) }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_IMAGES),
    ) { uris -> viewModel.addImages(uris) }
    fun pickImages() {
        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun shareText(text: String) {
        if (text.isBlank()) {
            Toast.makeText(context, context.getString(R.string.chat_export_empty), Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.chat_share_subject))
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.chat_share)))
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawer(
                conversations = state.conversations,
                activeId = state.activeConversationId,
                onNewChat = {
                    viewModel.newChat()
                    scope.launch { drawerState.close() }
                },
                onSelect = { id ->
                    viewModel.selectConversation(id)
                    scope.launch { drawerState.close() }
                },
                onRename = { renameTarget = it },
                onTogglePin = viewModel::togglePinned,
                onDelete = { deleteTarget = it },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    onOpenSettings()
                },
            )
        },
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = state.activeConversation?.title ?: stringResource(R.string.chat_title),
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Outlined.Menu,
                                contentDescription = stringResource(R.string.open_conversations),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = viewModel::newChat) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.new_chat),
                            )
                        }
                        IconButton(onClick = onOpenCall) {
                            Icon(
                                imageVector = Icons.Outlined.Phone,
                                contentDescription = stringResource(R.string.open_call),
                            )
                        }
                        Box {
                            IconButton(onClick = { showTopMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = stringResource(R.string.more),
                                )
                            }
                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chat_model_settings)) },
                                    onClick = {
                                        showTopMenu = false
                                        showModelDialog = true
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.chat_export)) },
                                    onClick = {
                                        showTopMenu = false
                                        shareText(buildTranscript(state.messages))
                                    },
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.settings)) },
                                    onClick = {
                                        showTopMenu = false
                                        onOpenSettings()
                                    },
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
            },
            bottomBar = {
                ChatInputBar(
                    value = state.input,
                    onValueChange = viewModel::onInputChange,
                    isStreaming = state.isStreaming,
                    apiKeyConfigured = state.apiKeyConfigured,
                    pendingImages = state.pendingImages,
                    isProcessingImages = state.isProcessingImages,
                    visionEnabled = state.visionEnabled,
                    onPickImages = ::pickImages,
                    onRemoveImage = viewModel::removePendingImage,
                    onSend = viewModel::send,
                    onStop = viewModel::stop,
                    onOpenSettings = onOpenSettings,
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                state.errorMessage?.let { message ->
                    ErrorBanner(message = message, onDismiss = viewModel::clearError)
                }
                if (state.messages.isEmpty()) {
                    EmptyChatState(
                        apiKeyConfigured = state.apiKeyConfigured,
                        onOpenSettings = onOpenSettings,
                    )
                } else {
                    MessageList(
                        messages = state.messages,
                        streamingAssistantId = state.streamingAssistantId,
                        streamingContent = state.streamingContent,
                        streamingReasoning = state.streamingReasoning,
                        isStreaming = state.isStreaming,
                        onCopy = { message ->
                            copyText(context, message.content)
                            Toast.makeText(context, R.string.chat_copy, Toast.LENGTH_SHORT).show()
                        },
                        onShare = { message -> shareText(message.content) },
                        onEdit = { editTarget = it },
                        onDelete = viewModel::deleteMessage,
                        onRegenerate = viewModel::regenerate,
                        onPreviewImage = { previewImage = it },
                    )
                }
            }
        }
    }

    renameTarget?.let { target ->
        RenameDialog(
            initial = target.title,
            onDismiss = { renameTarget = null },
            onConfirm = { title ->
                viewModel.renameConversation(target.id, title)
                renameTarget = null
            },
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.chat_delete_conversation_title)) },
            text = { Text(stringResource(R.string.chat_delete_conversation_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteConversation(target.id)
                        deleteTarget = null
                    },
                ) {
                    Text(stringResource(R.string.chat_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.chat_cancel))
                }
            },
        )
    }
    editTarget?.let { target ->
        EditMessageDialog(
            initial = target.content,
            onDismiss = { editTarget = null },
            onConfirm = { content ->
                viewModel.editMessage(target.id, content)
                editTarget = null
            },
        )
    }
    if (showModelDialog) {
        ModelSettingsDialog(
            models = state.models,
            currentModel = state.currentModelInfo,
            currentEffort = state.activeConversation?.reasoningEffort ?: ReasoningEffort.HIGH,
            currentMaxTokens = state.activeConversation?.maxOutputTokens ?: 8192,
            onRefresh = viewModel::refreshModels,
            onDismiss = { showModelDialog = false },
            onApply = { model, effort, maxTokens ->
                viewModel.updateConversationConfig(model, effort, maxTokens)
                showModelDialog = false
            },
        )
    }
    previewImage?.let { path ->
        FullScreenImageDialog(path = path, onDismiss = { previewImage = null })
    }
}

@Composable
private fun ConversationDrawer(
    conversations: List<Conversation>,
    activeId: String?,
    onNewChat: () -> Unit,
    onSelect: (String) -> Unit,
    onRename: (Conversation) -> Unit,
    onTogglePin: (String) -> Unit,
    onDelete: (Conversation) -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalDrawerSheet {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.conversations),
                style = MaterialTheme.typography.headlineLarge,
            )
            Spacer(Modifier.height(16.dp))
            SoftSeekButton(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.new_chat),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            Spacer(Modifier.height(16.dp))
            if (conversations.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_conversations),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(conversations, key = { it.id }) { conversation ->
                        ConversationRow(
                            conversation = conversation,
                            selected = conversation.id == activeId,
                            onClick = { onSelect(conversation.id) },
                            onRename = { onRename(conversation) },
                            onTogglePin = { onTogglePin(conversation.id) },
                            onDelete = { onDelete(conversation) },
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.softSeekColors.divider)
            SettingItem(
                title = stringResource(R.string.settings),
                icon = Icons.Outlined.Settings,
                onClick = onOpenSettings,
            )
            Text(
                text = stringResource(R.string.app_subtitle),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.softSeekColors.textSecondary,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Surface(
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.Star,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (conversation.isPinned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                },
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = conversation.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                )
                conversation.lastMessagePreview?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.softSeekColors.textSecondary,
                        maxLines = 1,
                    )
                }
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = stringResource(R.string.more),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_rename_title)) },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (conversation.isPinned) R.string.chat_unpin else R.string.chat_pin,
                                ),
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onTogglePin()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_delete)) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessage>,
    streamingAssistantId: String?,
    streamingContent: String,
    streamingReasoning: String,
    isStreaming: Boolean,
    onCopy: (ChatMessage) -> Unit,
    onShare: (ChatMessage) -> Unit,
    onEdit: (ChatMessage) -> Unit,
    onDelete: (String) -> Unit,
    onRegenerate: () -> Unit,
    onPreviewImage: (String) -> Unit,
) {
    val visible = messages.filter { !it.isDeleted && it.role != MessageRole.SYSTEM }
    val lastAssistantId = visible.lastOrNull { it.role == MessageRole.ASSISTANT }?.id
    val listState = rememberLazyListState()
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= visible.lastIndex - 1
        }
    }
    LaunchedEffect(visible.size, streamingContent.length, streamingReasoning.length) {
        if (visible.isNotEmpty() && atBottom) {
            listState.scrollToItem(visible.lastIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(visible, key = { it.id }) { message ->
            val streaming = message.id == streamingAssistantId && isStreaming
            MessageBubble(
                message = message,
                isStreaming = streaming,
                streamingContent = if (streaming) streamingContent else message.content,
                streamingReasoning = if (streaming) streamingReasoning else message.reasoningContent.orEmpty(),
                canRegenerate = message.id == lastAssistantId && !isStreaming,
                onCopy = { onCopy(message) },
                onShare = { onShare(message) },
                onEdit = { onEdit(message) },
                onDelete = { onDelete(message.id) },
                onRegenerate = onRegenerate,
                onRetry = onRegenerate,
                onPreviewImage = onPreviewImage,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    isStreaming: Boolean,
    streamingContent: String,
    streamingReasoning: String,
    canRegenerate: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit,
    onRetry: () -> Unit,
    onPreviewImage: (String) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isUser = message.role == MessageRole.USER
    val content = streamingContent.ifBlank { message.content }
    val reasoning = streamingReasoning.ifBlank { message.reasoningContent.orEmpty() }
    val imageAttachments = message.attachments.filter { it.type == AttachmentType.IMAGE }
    val finishWarning = if (isUser || isStreaming) {
        null
    } else {
        when (message.finishReason) {
            "length" -> stringResource(R.string.chat_finish_length)
            "content_filter" -> stringResource(R.string.chat_finish_filter)
            "insufficient_system_resource" -> stringResource(R.string.chat_finish_resource)
            else -> null
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        ) {
            StyleAwareBubble(
                isMine = isUser,
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { menuOpen = true },
                    ),
            ) {
                Column {
                    if (imageAttachments.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(imageAttachments, key = { it.attachmentId }) { attachment ->
                                AsyncImage(
                                    model = File(attachment.localPath),
                                    contentDescription = stringResource(R.string.chat_image),
                                    modifier = Modifier
                                        .size(132.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onPreviewImage(attachment.localPath) },
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                        if (content.isNotBlank()) Spacer(Modifier.height(8.dp))
                    }
                    if (!isUser) {
                        Text(
                            text = "SOFTSEEK",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (!isUser && reasoning.isNotBlank()) {
                        ThinkingBlock(text = reasoning, streaming = isStreaming)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (content.isBlank() && isStreaming) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.chat_thinking),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.softSeekColors.textSecondary,
                            )
                        }
                    } else if (isUser) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        MarkdownText(text = content)
                    }
                    if (message.errorMessage != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.chat_error_label, message.errorMessage),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    finishWarning?.let { warning ->
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (message.status == MessageStatus.FAILED) {
                        TextButton(onClick = onRetry) {
                            Text(stringResource(R.string.chat_retry))
                        }
                    }
                    if (!isStreaming && message.status == MessageStatus.PARTIAL && content.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.chat_stopped),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.softSeekColors.textSecondary,
                        )
                    }
                    if (message.totalTokens > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = pluralStringResource(
                                R.plurals.chat_tokens,
                                message.totalTokens,
                                message.totalTokens,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) {
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                            } else {
                                MaterialTheme.softSeekColors.textSecondary
                            },
                        )
                    }
                }
            }
        }
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = { menuOpen = false },
            modifier = Modifier.align(if (isUser) Alignment.BottomEnd else Alignment.BottomStart),
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_copy)) },
                onClick = {
                    menuOpen = false
                    onCopy()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_share)) },
                onClick = {
                    menuOpen = false
                    onShare()
                },
            )
            if (isUser) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_edit)) },
                    onClick = {
                        menuOpen = false
                        onEdit()
                    },
                )
            }
            if (!isUser && canRegenerate) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.chat_regenerate)) },
                    onClick = {
                        menuOpen = false
                        onRegenerate()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.chat_delete)) },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun ThinkingBlock(
    text: String,
    streaming: Boolean,
) {
    var expanded by remember(streaming) { mutableStateOf(streaming) }
    Surface(
        color = MaterialTheme.softSeekColors.thinkingBackground,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (streaming) R.string.chat_thinking else R.string.chat_thinking_done),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (expanded) {
                        Icons.Outlined.KeyboardArrowUp
                    } else {
                        Icons.Outlined.KeyboardArrowDown
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            if (expanded) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isStreaming: Boolean,
    apiKeyConfigured: Boolean,
    pendingImages: List<MessageAttachment>,
    isProcessingImages: Boolean,
    visionEnabled: Boolean,
    onPickImages: () -> Unit,
    onRemoveImage: (String) -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val inputRadius = LocalSoftSeekUiSettings.current.bubbleRadiusDp.dp
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            if (!apiKeyConfigured) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.not_configured),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.softSeekColors.textSecondary,
                    )
                    Spacer(Modifier.width(8.dp))
                    AssistChip(
                        onClick = onOpenSettings,
                        label = { Text(stringResource(R.string.chat_configure_key)) },
                    )
                }
            }
            if (pendingImages.isNotEmpty() || isProcessingImages) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(pendingImages, key = { it.attachmentId }) { attachment ->
                        Box {
                            AsyncImage(
                                model = File(attachment.localPath),
                                contentDescription = stringResource(R.string.chat_image),
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            IconButton(
                                onClick = { onRemoveImage(attachment.attachmentId) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.chat_remove_image),
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                    if (isProcessingImages) {
                        item {
                            Box(
                                modifier = Modifier.size(64.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                IconButton(onClick = onPickImages, enabled = visionEnabled && !isStreaming) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.attach),
                    )
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.input_hint)) },
                    maxLines = 6,
                    shape = RoundedCornerShape(inputRadius),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (value.isNotBlank() && !isStreaming) onSend()
                        },
                    ),
                )
                Spacer(Modifier.width(8.dp))
                if (isStreaming) {
                    SoftSeekIconButton(
                        onClick = onStop,
                        primary = false,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.chat_stop),
                        )
                    }
                } else {
                    SoftSeekIconButton(
                        onClick = onSend,
                        enabled = value.isNotBlank() || pendingImages.isNotEmpty(),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = stringResource(R.string.send),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyChatState(
    apiKeyConfigured: Boolean,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(R.drawable.softseek_logo_mark),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .height(72.dp)
                .widthIn(max = 220.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.chat_empty_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.chat_empty_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.softSeekColors.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        AssistChip(
            onClick = onOpenSettings,
            label = {
                Text(
                    stringResource(
                        if (apiKeyConfigured) R.string.chat_key_ready else R.string.chat_configure_key,
                    ),
                )
            },
        )
    }
}

@Composable
private fun ErrorBanner(
    message: String,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.chat_cancel),
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var title by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_rename_title)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.chat_rename_hint)) },
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(title) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.chat_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_cancel))
            }
        },
    )
}

@Composable
private fun EditMessageDialog(
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var content by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_edit_message_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.chat_edit_message_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.softSeekColors.textSecondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(content) }, enabled = content.isNotBlank()) {
                Text(stringResource(R.string.chat_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.chat_cancel))
            }
        },
    )
}

@Composable
private fun ModelSettingsDialog(
    models: List<AiModelInfo>,
    currentModel: AiModelInfo?,
    currentEffort: ReasoningEffort,
    currentMaxTokens: Int,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onApply: (AiModelInfo, ReasoningEffort, Int) -> Unit,
) {
    val available = models.ifEmpty { listOfNotNull(currentModel) }
    var selected by remember(currentModel?.id) {
        mutableStateOf(available.firstOrNull { it.id == currentModel?.id })
    }
    var effort by remember(currentEffort) { mutableStateOf(currentEffort) }
    var maxTokens by remember(currentMaxTokens) { mutableStateOf(currentMaxTokens.toString()) }
    var manualModelId by remember(currentModel?.id) { mutableStateOf("") }
    LaunchedEffect(Unit) { onRefresh() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_model_title)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (available.isEmpty()) {
                    Text(
                        text = "模型列表为空。可在下方手动填写 OpenAI 兼容模型 ID。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.softSeekColors.textSecondary,
                    )
                } else {
                    available.forEach { model ->
                        FilterChip(
                            selected = model.id == selected?.id,
                            onClick = { selected = model },
                            label = {
                                Text(
                                    "${model.displayName} · ${if (model.inputModalities.contains("image")) {
                                        "图片+文本"
                                    } else {
                                        "纯文本"
                                    }}",
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.settings_thinking_effort),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ReasoningEffort.entries.forEach { level ->
                        val supported = selected == null ||
                            selected?.reasoningLevels?.isEmpty() == true ||
                            level == ReasoningEffort.NONE ||
                            selected?.reasoningLevels?.contains(level) == true
                        if (supported) {
                            FilterChip(
                                selected = effort == level,
                                onClick = { effort = level },
                                label = { Text(effortLabel(level)) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = manualModelId,
                    onValueChange = { manualModelId = it.trim() },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("手动模型 ID（可选）") },
                    placeholder = { Text("例如 gpt-4o、deepseek-chat") },
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = maxTokens,
                    onValueChange = { maxTokens = it.filter(Char::isDigit).take(6) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.settings_max_output)) },
                    supportingText = {
                        Text("$MIN_OUTPUT_TOKENS - ${selected?.maxOutputTokens?.takeIf { it > 0 } ?: 393216}")
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val manual = manualModelId.trim()
                    val model = if (manual.isNotBlank()) {
                        AiModelInfo(
                            id = manual,
                            displayName = manual,
                            contextWindow = 0,
                            maxOutputTokens = 393_216,
                            inputModalities = listOf("text", "image"),
                            reasoningLevels = emptyList(),
                            defaultReasoningEffort = null,
                        )
                    } else {
                        selected
                    }
                    if (model != null) {
                        val tokens = maxTokens.toIntOrNull() ?: currentMaxTokens
                        val upper = model.maxOutputTokens.takeIf { it > 0 } ?: 393216
                        onApply(model, effort, tokens.coerceIn(MIN_OUTPUT_TOKENS, upper))
                    }
                },
            ) {
                Text(stringResource(R.string.chat_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.chat_cancel)) }
        },
    )
}

private fun effortLabel(value: ReasoningEffort): String = when (value) {
    ReasoningEffort.NONE -> "关闭思考"
    ReasoningEffort.LOW -> "低"
    ReasoningEffort.HIGH -> "高"
    ReasoningEffort.MAX -> "最大"
}

@Composable
private fun FullScreenImageDialog(
    path: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = File(path),
                contentDescription = stringResource(R.string.chat_image),
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Fit,
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.chat_cancel),
                    tint = Color.White,
                )
            }
        }
    }
}

private const val MAX_IMAGES = 8
private const val MIN_OUTPUT_TOKENS = 256

private fun copyText(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("SOFTSEEK", text))
}

private fun buildTranscript(messages: List<ChatMessage>): String = messages
    .filter { !it.isDeleted && it.role != MessageRole.SYSTEM }
    .joinToString("\n\n") { message ->
        val role = if (message.role == MessageRole.USER) "我" else "SOFTSEEK"
        "$role:\n${message.content}"
    }
