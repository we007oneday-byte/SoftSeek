package io.github.we007oneday_byte.softseek.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通话音频模式：进入时切到 MODE_IN_COMMUNICATION 并申请音频焦点，
 * 退出时恢复进入前的模式、扬声器状态并放弃焦点，避免影响其他应用。
 */
@Singleton
class CallAudioController
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val audioManager = context.getSystemService(AudioManager::class.java)
        private var previousMode = AudioManager.MODE_NORMAL
        private var previousSpeaker = false
        private var focusRequest: AudioFocusRequest? = null
        private var entered = false

        val speakerOn: Boolean
            get() = audioManager?.let { manager ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    manager.communicationDevice?.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                } else {
                    @Suppress("DEPRECATION")
                    manager.isSpeakerphoneOn
                }
            } == true

        fun enterCallMode(): Result<Unit> {
            val manager = audioManager ?: return Result.failure(IllegalStateException("AudioManager 不可用"))
            return runCatching {
                if (!entered) {
                    previousMode = manager.mode
                    previousSpeaker = speakerOn
                    entered = true
                }
                manager.mode = AudioManager.MODE_IN_COMMUNICATION
                setSpeakerOn(false)
                requestFocus(manager)
            }
        }

        fun setSpeakerOn(on: Boolean) {
            val manager = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val targetType = if (on) {
                    AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                } else {
                    AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
                }
                val device = manager.availableCommunicationDevices.firstOrNull {
                    it.type == targetType
                }
                if (device != null) {
                    manager.setCommunicationDevice(device)
                } else {
                    @Suppress("DEPRECATION")
                    manager.isSpeakerphoneOn = on
                }
            } else {
                @Suppress("DEPRECATION")
                manager.isSpeakerphoneOn = on
            }
        }

        fun exitCallMode() {
            val manager = audioManager ?: return
            if (!entered) return
            entered = false
            focusRequest?.let { manager.abandonAudioFocusRequest(it) }
            focusRequest = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                manager.clearCommunicationDevice()
            }
            setSpeakerOn(previousSpeaker)
            manager.mode = previousMode
        }

        private fun requestFocus(manager: AudioManager) {
            if (focusRequest != null) return
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setAcceptsDelayedFocusGain(false)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener { }
                .build()
            focusRequest = request
            manager.requestAudioFocus(request)
        }
    }
