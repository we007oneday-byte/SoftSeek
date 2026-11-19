package io.github.we007oneday_byte.softseek.core.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import kotlin.math.max

/**
 * 16kHz / 16bit / mono 采集。优先 VOICE_COMMUNICATION（系统通话链路通常自带 AEC/NS），
 * 失败回退 MIC；可用时再显式打开 AcousticEchoCanceler。
 */
class AudioCapture(
    private val sampleRate: Int = SAMPLE_RATE,
) {
    private var record: AudioRecord? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    var sourceLabel: String = "未启动"
        private set

    val isActive: Boolean
        get() = record?.recordingState == AudioRecord.RECORDSTATE_RECORDING

    @SuppressLint("MissingPermission")
    fun start(): Result<Unit> {
        stop()
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC,
        )
        var lastError: Exception? = null
        for (source in sources) {
            var candidate: AudioRecord? = null
            try {
                candidate = buildRecord(source)
                if (candidate.state != AudioRecord.STATE_INITIALIZED) {
                    candidate.release()
                    continue
                }
                candidate.startRecording()
                if (candidate.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    candidate.release()
                    continue
                }
                record = candidate
                candidate = null
                sourceLabel = if (source == MediaRecorder.AudioSource.VOICE_COMMUNICATION) {
                    "VOICE_COMMUNICATION"
                } else {
                    "MIC"
                }
                enableAec(record ?: continue)
                return Result.success(Unit)
            } catch (error: Exception) {
                candidate?.release()
                candidate = null
                record?.release()
                record = null
                lastError = error
            }
        }
        val reason = lastError?.message ?: "设备不支持 16kHz 单声道采集"
        return Result.failure(
            IllegalStateException(
                "无法启动麦克风：$reason。请确认已授予麦克风权限，且没有被其他应用占用。",
                lastError,
            ),
        )
    }

    @SuppressLint("MissingPermission")
    fun read(samples: ShortArray): Int {
        val current = record ?: return -1
        return try {
            current.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
        } catch (_: Exception) {
            -1
        }
    }

    fun stop() {
        echoCanceler?.let {
            runCatching { it.enabled = false }
            runCatching { it.release() }
        }
        echoCanceler = null
        record?.let {
            runCatching { it.stop() }
            it.release()
        }
        record = null
        sourceLabel = "未启动"
    }

    fun release() = stop()

    @SuppressLint("MissingPermission")
    private fun buildRecord(source: Int): AudioRecord {
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        require(minBuffer > 0) { "设备不支持 16kHz 单声道 PCM 采集" }
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()
        return AudioRecord.Builder()
            .setAudioSource(source)
            .setAudioFormat(format)
            .setBufferSizeInBytes(max(minBuffer * 2, BUFFER_BYTES))
            .build()
    }

    private fun enableAec(record: AudioRecord) {
        if (!AcousticEchoCanceler.isAvailable()) return
        runCatching {
            AcousticEchoCanceler.create(record.audioSessionId)?.also {
                it.enabled = true
                echoCanceler = it
            }
        }
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        private const val BUFFER_BYTES = 4 * 1024
    }
}
