package io.github.we007oneday_byte.softseek.core.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.graphics.scale
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.we007oneday_byte.softseek.domain.model.AttachmentType
import io.github.we007oneday_byte.softseek.domain.model.MessageAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class ImageProcessor
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val imageDir: File
            get() = File(context.filesDir, IMAGE_DIR).apply { mkdirs() }

        suspend fun prepare(uris: List<Uri>, startOrder: Int = 0): List<MessageAttachment> =
            withContext(Dispatchers.IO) {
                uris.take(MAX_IMAGES).mapIndexed { index, uri ->
                    process(uri, startOrder + index)
                }
            }

        fun clearCache(): Int {
            var deleted = 0
            imageDir.listFiles()?.forEach { file ->
                if (file.delete()) deleted++
            }
            return deleted
        }

        private fun process(uri: Uri, sortOrder: Int): MessageAttachment {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                ?: throw IOException("无法读取图片")
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("图片格式不受支持")

            val sample = sampleSize(bounds.outWidth, bounds.outHeight)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IOException("图片解码失败")
            val oriented = orient(decoded, readOrientation(uri))
            val scaled = scaleIfNeeded(oriented)
            val output = File(imageDir, "${UUID.randomUUID()}.jpg")
            output.outputStream().use { stream ->
                scaled.compress(Bitmap.CompressFormat.JPEG, QUALITY, stream)
            }
            val width = scaled.width
            val height = scaled.height
            if (decoded !== scaled) decoded.recycle()
            if (oriented !== decoded && oriented !== scaled) oriented.recycle()
            if (scaled !== oriented) scaled.recycle()
            pruneCache()
            return MessageAttachment(
                attachmentId = UUID.randomUUID().toString(),
                nodeId = "",
                type = AttachmentType.IMAGE,
                localPath = output.absolutePath,
                mime = "image/jpeg",
                width = width,
                height = height,
                sizeBytes = output.length(),
                sortOrder = sortOrder,
            )
        }

        private fun sampleSize(width: Int, height: Int): Int {
            var sample = 1
            while (max(width, height) / sample > MAX_DIMENSION * 2) {
                sample *= 2
            }
            return sample
        }

        private fun scaleIfNeeded(bitmap: Bitmap): Bitmap {
            val longest = max(bitmap.width, bitmap.height)
            if (longest <= MAX_DIMENSION) return bitmap
            val ratio = MAX_DIMENSION.toFloat() / longest
            val width = (bitmap.width * ratio).toInt().coerceAtLeast(1)
            val height = (bitmap.height * ratio).toInt().coerceAtLeast(1)
            return bitmap.scale(width, height, true)
        }

        private fun orient(bitmap: Bitmap, orientation: Int): Bitmap {
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
                else -> return bitmap
            }
            return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }

        private fun readOrientation(uri: Uri): Int {
            val resolver = context.contentResolver
            return resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }

        private fun pruneCache() {
            val files = imageDir.listFiles()?.sortedBy { it.lastModified() } ?: return
            var total = files.sumOf { it.length() }
            for (file in files) {
                if (total <= MAX_CACHE_BYTES) break
                total -= file.length()
                file.delete()
            }
        }

        private companion object {
            const val IMAGE_DIR = "chat_images"
            const val MAX_DIMENSION = 1568
            const val QUALITY = 85
            const val MAX_IMAGES = 8
            const val MAX_CACHE_BYTES = 100L * 1024 * 1024
        }
    }
