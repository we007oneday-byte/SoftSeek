package io.github.we007oneday_byte.softseek.ui.screens.call

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.we007oneday_byte.softseek.R
import io.github.we007oneday_byte.softseek.ui.theme.LocalIsLowEndDevice
import io.github.we007oneday_byte.softseek.ui.theme.LocalSoftSeekUiSettings
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareButton
import io.github.we007oneday_byte.softseek.ui.theme.StyleAwareCard

@Composable
fun CallScreen(onEndCall: () -> Unit) {
    val viewModel: CallViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val uiSettings = LocalSoftSeekUiSettings.current
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            permissionDenied = false
            viewModel.startCall()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            permissionDenied = true
        }
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.startCall()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    DisposableEffect(Unit) {
        onDispose { viewModel.endCall() }
    }
    ProximityEffect()

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "AI 通话",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = formatDuration(state.durationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = when (state.phase) {
                        CallPhase.DIALING -> "准备中"
                        CallPhase.LISTENING -> "聆听"
                        CallPhase.THINKING -> "思考"
                        CallPhase.SPEAKING -> "回答"
                        CallPhase.ENDED -> "已结束"
                        CallPhase.ERROR -> "异常"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(20.dp))
            StyleAwareCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    VoiceVisualizerCanvas(
                        volume = if (state.phase == CallPhase.LISTENING && !state.muted) 0.72f else 0.16f,
                        style = settings.voiceAnimationStyle,
                        isLowEndDevice = LocalIsLowEndDevice.current,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = when (state.phase) {
                            CallPhase.DIALING -> "正在准备离线模型与麦克风…"
                            CallPhase.LISTENING -> if (state.muted) {
                                "已静音"
                            } else if (state.selfTestMode) {
                                "正在聆听 · 本地自检"
                            } else {
                                "正在聆听…"
                            }
                            CallPhase.THINKING -> "思考中…"
                            CallPhase.SPEAKING -> "正在回答…"
                            CallPhase.ENDED -> "通话已结束"
                            CallPhase.ERROR -> "通话异常"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    if (state.selfTestMode) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "本地自检模式 · 未配置 API Key\n说一句话，将用固定提示验证麦克风、离线识别和 TTS。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    PartialTranscript(
                        text = state.partialText,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (state.lastUserText.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "你说 · ${state.lastUserText}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (state.assistantText.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        StyleAwareCard(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column {
                                Text(
                                    text = "SOFTSEEK",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(Modifier.height(4.dp))
                                StreamingAssistantText(
                                    text = state.assistantText,
                                    spokenSentenceIndex = state.spokenSentenceIndex,
                                    messageKey = state.lastUserText,
                                )
                            }
                        }
                    }
                    state.error?.let {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    if (permissionDenied) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "需要麦克风权限才能通话；授权后才能启动离线识别。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                        TextButton(
                            onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                        ) {
                            Text("重新授权麦克风")
                        }
                        TextButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = "package:${context.packageName}".toUri()
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            },
                        ) {
                            Text("打开系统设置")
                        }
                    }
                }
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
                        state.phase != CallPhase.DIALING &&
                        state.phase != CallPhase.THINKING &&
                        state.phase != CallPhase.SPEAKING &&
                        state.phase != CallPhase.ERROR &&
                        state.phase != CallPhase.ENDED,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("发送")
                }
                Spacer(Modifier.height(8.dp))
            }
            if (state.phase == CallPhase.THINKING) {
                TextButton(onClick = viewModel::cancelResponse) {
                    Text("停止生成")
                }
            }
            if (state.phase == CallPhase.SPEAKING) {
                TextButton(onClick = viewModel::interruptSpeaking) {
                    Text("打断播报")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StyleAwareButton(
                    onClick = viewModel::toggleMute,
                    modifier = Modifier.weight(1f),
                    enabled = state.phase != CallPhase.ENDED && state.phase != CallPhase.ERROR,
                    primary = false,
                ) {
                    Text(if (state.muted) "取消静音" else "静音")
                }
                StyleAwareButton(
                    onClick = viewModel::toggleSpeaker,
                    modifier = Modifier.weight(1f),
                    enabled = state.phase != CallPhase.ENDED && state.phase != CallPhase.ERROR,
                    primary = false,
                ) {
                    Text(if (state.speakerOn) "听筒" else "免提")
                }
                StyleAwareButton(
                    onClick = viewModel::toggleKeyboard,
                    modifier = Modifier.weight(1f),
                    enabled = state.phase != CallPhase.DIALING &&
                        state.phase != CallPhase.ENDED &&
                        state.phase != CallPhase.ERROR,
                    primary = false,
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
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Phone,
                        contentDescription = stringResource(R.string.end_call),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.end_call),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProximityEffect() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    DisposableEffect(activity) {
        if (activity == null) {
            onDispose { }
        } else {
            val window = activity.window
            val sensorManager = activity.getSystemService(SensorManager::class.java)
            val proximity = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val near = event.values.firstOrNull()?.let { value ->
                        value < (event.sensor.maximumRange.takeIf { it > 0f } ?: 5f)
                    } == true
                    val attributes = window.attributes
                    if (near) {
                        attributes.screenBrightness = 0.01f
                        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        attributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    window.attributes = attributes
                }

                override fun onAccuracyChanged(
                    sensor: Sensor?,
                    accuracy: Int,
                ) = Unit
            }
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            proximity?.let {
                sensorManager?.registerListener(listener, it, SensorManager.SENSOR_DELAY_NORMAL)
            }
            onDispose {
                sensorManager?.unregisterListener(listener)
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val attributes = window.attributes
                attributes.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = attributes
            }
        }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val remaining = seconds % 60
    return "%02d:%02d".format(minutes, remaining)
}
