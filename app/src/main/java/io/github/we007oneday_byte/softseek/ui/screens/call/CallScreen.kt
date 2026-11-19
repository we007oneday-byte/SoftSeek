package io.github.we007oneday_byte.softseek.ui.screens.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.we007oneday_byte.softseek.R

@Composable
fun CallScreen(onEndCall: () -> Unit) {
    val viewModel: CallViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startCall()
        } else {
            permissionDenied = true
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startCall()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.endCall() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1B1733), Color(0xFF0E0E14)),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.call_title),
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatDuration(state.durationSeconds),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
            )
            Spacer(Modifier.height(32.dp))
            Text(
                text = when (state.phase) {
                    CallPhase.DIALING -> "正在准备离线模型与麦克风…"
                    CallPhase.LISTENING -> if (state.muted) "已静音" else "正在聆听…"
                    CallPhase.THINKING -> "思考中…"
                    CallPhase.SPEAKING -> "正在回答…"
                    CallPhase.ENDED -> "通话已结束"
                    CallPhase.ERROR -> "通话异常"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            if (state.partialText.isNotBlank()) {
                Text(
                    text = "“${state.partialText}”",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFB9A7FF),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }
            if (state.lastUserText.isNotBlank()) {
                LabeledText(label = "你说", text = state.lastUserText)
                Spacer(Modifier.height(12.dp))
            }
            if (state.assistantText.isNotBlank()) {
                LabeledText(label = "SOFTSEEK", text = state.assistantText)
            }
            state.error?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center,
                )
            }
            if (permissionDenied) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "需要麦克风权限才能通话；可在系统设置中重新授权。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFF8A80),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.weight(1f))
            if (state.showKeyboard) {
                OutlinedTextField(
                    value = state.inputText,
                    onValueChange = viewModel::onInputChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("输入文字，代替这一轮语音") },
                )
                TextButton(
                    onClick = viewModel::sendText,
                    enabled = state.inputText.isNotBlank() &&
                        state.phase != CallPhase.THINKING &&
                        state.phase != CallPhase.SPEAKING &&
                        state.phase != CallPhase.ENDED,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("发送")
                }
                Spacer(Modifier.height(8.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = viewModel::toggleMute,
                    modifier = Modifier.weight(1f),
                    enabled = state.phase != CallPhase.ENDED && state.phase != CallPhase.ERROR,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.14f),
                        contentColor = Color.White,
                    ),
                ) {
                    Text(if (state.muted) "取消静音" else "静音")
                }
                Button(
                    onClick = viewModel::toggleKeyboard,
                    modifier = Modifier.weight(1f),
                    enabled = state.phase != CallPhase.ENDED,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.14f),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("键盘")
                }
                Button(
                    onClick = {
                        viewModel.endCall()
                        onEndCall()
                    },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = stringResource(R.string.end_call),
                        tint = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.end_call),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun LabeledText(
    label: String,
    text: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.5f),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "%02d:%02d".format(minutes, remaining)
}
