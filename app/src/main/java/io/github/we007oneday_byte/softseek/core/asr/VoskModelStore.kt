package io.github.we007oneday_byte.softseek.core.asr

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * 只负责本地模型文件：路径、完整性校验、安全解压和原子替换。
 * 不依赖 Android，可在 JVM 单测中覆盖 zip slip、超大条目和校验失败。
 */
class VoskModelStore(
    private val modelsDir: File,
) {
    fun modelDir(spec: VoskModelSpec): File = File(modelsDir, spec.rootDirName)

    fun archivePart(spec: VoskModelSpec): File = File(modelsDir, "${spec.id}.zip.part")

    fun inspect(spec: VoskModelSpec): VoskModelState =
        if (isInstalled(spec)) {
            VoskModelState.Installed(modelDir(spec).absolutePath)
        } else {
            VoskModelState.NotInstalled
        }

    fun isInstalled(spec: VoskModelSpec): Boolean {
        val dir = modelDir(spec)
        return dir.isDirectory && spec.requiredPaths.all { File(dir, it).isFile }
    }

    fun verifyArchive(
        archive: File,
        spec: VoskModelSpec,
    ): Result<Unit> {
        if (!archive.isFile) {
            return Result.failure(IllegalArgumentException("模型压缩包不存在"))
        }
        if (archive.length() != spec.archiveBytes) {
            return Result.failure(
                IllegalArgumentException(
                    "模型大小不匹配：期望 ${spec.archiveBytes}，实际 ${archive.length()}",
                ),
            )
        }
        if (spec.sha256.isBlank()) {
            return Result.failure(IllegalArgumentException("模型规格缺少 SHA-256，拒绝安装"))
        }
        val actual = sha256(archive)
        if (!actual.equals(spec.sha256, ignoreCase = true)) {
            return Result.failure(
                IllegalArgumentException(
                    "模型 SHA-256 不匹配：期望 ${spec.sha256}，实际 $actual",
                ),
            )
        }
        return Result.success(Unit)
    }

    /**
     * 先解压到私有 staging 目录，校验 requiredPaths 后再替换正式目录。
     * 任何异常都会清理 staging，不留下半安装模型。
     */
    fun extractArchive(
        archive: File,
        spec: VoskModelSpec,
    ): Result<File> {
        val staging = File(modelsDir, ".staging-${spec.id}")
        val old = File(modelsDir, ".old-${spec.id}")
        return try {
            modelsDir.mkdirs()
            deleteTree(staging)
            deleteTree(old)
            staging.mkdirs()

            var totalBytes = 0L
            var entryCount = 0
            ZipInputStream(BufferedInputStream(FileInputStream(archive))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount += 1
                    if (entryCount > MAX_ENTRIES) {
                        throw IllegalArgumentException("压缩包条目过多")
                    }
                    val name = entry.name.replace('\\', '/')
                    if (name.startsWith("/") || name.contains("..") || DRIVE_PREFIX.containsMatchIn(name)) {
                        throw IllegalArgumentException("压缩包包含非法路径: $name")
                    }
                    val target = File(staging, name)
                    if (!target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) {
                        throw IllegalArgumentException("压缩包路径越界: $name")
                    }
                    if (entry.isDirectory) {
                        target.mkdirs()
                        continue
                    }
                    target.parentFile?.mkdirs()
                    var entryBytes = 0L
                    FileOutputStream(target).use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            entryBytes += read
                            totalBytes += read
                            if (entryBytes > MAX_ENTRY_BYTES || totalBytes > MAX_UNCOMPRESSED_BYTES) {
                                throw IllegalArgumentException("压缩包解压后体积超限")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                    zip.closeEntry()
                }
            }

            val extractedRoot = File(staging, spec.rootDirName)
            if (!extractedRoot.isDirectory) {
                throw IllegalArgumentException("压缩包缺少模型根目录 ${spec.rootDirName}")
            }
            val missing = spec.requiredPaths.filterNot { File(extractedRoot, it).isFile }
            if (missing.isNotEmpty()) {
                throw IllegalArgumentException("模型缺少必要文件: ${missing.joinToString()}")
            }

            val target = modelDir(spec)
            if (target.exists() && !target.renameTo(old)) {
                throw IllegalStateException("无法替换旧模型目录")
            }
            if (!extractedRoot.renameTo(target)) {
                old.renameTo(target)
                throw IllegalStateException("无法安装模型目录")
            }
            deleteTree(old)
            deleteTree(staging)
            Result.success(target)
        } catch (error: Exception) {
            deleteTree(staging)
            Result.failure(error)
        }
    }

    companion object {
        private const val BUFFER_SIZE = 8 * 1024
        private const val MAX_ENTRIES = 10_000
        private const val MAX_ENTRY_BYTES = 200L * 1024 * 1024
        private const val MAX_UNCOMPRESSED_BYTES = 250L * 1024 * 1024
        private val DRIVE_PREFIX = Regex("^[A-Za-z]:")

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }

        private fun deleteTree(file: File) {
            if (!file.exists()) return
            val children = file.listFiles()
            if (children != null) {
                children.forEach(::deleteTree)
            }
            file.delete()
        }
    }
}
