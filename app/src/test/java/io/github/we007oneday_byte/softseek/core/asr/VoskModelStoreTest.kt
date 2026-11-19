package io.github.we007oneday_byte.softseek.core.asr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class VoskModelStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun specFor(
        archive: File,
        sha256: String = VoskModelStore.sha256(archive),
        id: String = "test-model",
        rootDirName: String = "test-model",
    ) = VoskModelSpec(
        id = id,
        displayName = "测试模型",
        downloadUrl = "https://example.com/model.zip",
        archiveBytes = archive.length(),
        sha256 = sha256,
        rootDirName = rootDirName,
        requiredPaths = listOf("am/final.mdl", "conf/mfcc.conf"),
    )

    @Test
    fun ensureModelsDir_createsNestedDirectoryBeforeDownload() {
        val models = File(temporaryFolder.root, "files/models")
        val store = VoskModelStore(models)

        val result = store.ensureModelsDir()

        assertTrue(result.isSuccess)
        assertTrue(models.isDirectory)
    }

    @Test
    fun ensureModelsDir_reportsBlockedPath() {
        val blocked = temporaryFolder.newFile("models")
        val store = VoskModelStore(blocked)

        val result = store.ensureModelsDir()

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("无法创建"))
    }

    @Test
    fun sha256_matchesKnownVector() {
        val file = temporaryFolder.newFile("abc.txt").apply { writeText("abc") }

        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            VoskModelStore.sha256(file),
        )
    }

    @Test
    fun verifyArchive_rejectsWrongSize() {
        val archive = temporaryFolder.newFile("model.zip").apply { writeText("not-a-model") }
        val spec = specFor(archive).copy(archiveBytes = archive.length() + 1)

        val result = VoskModelStore(temporaryFolder.newFolder("models")).verifyArchive(archive, spec)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("大小"))
    }

    @Test
    fun verifyArchive_rejectsMissingChecksum() {
        val archive = temporaryFolder.newFile("model.zip").apply { writeText("model") }
        val spec = specFor(archive).copy(sha256 = "")

        val result = VoskModelStore(temporaryFolder.newFolder("models")).verifyArchive(archive, spec)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("SHA-256"))
    }

    @Test
    fun verifyArchive_rejectsHashMismatch() {
        val archive = temporaryFolder.newFile("model.zip").apply { writeText("model") }
        val spec = specFor(archive).copy(sha256 = "0".repeat(64))

        val result = VoskModelStore(temporaryFolder.newFolder("models")).verifyArchive(archive, spec)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("不匹配"))
    }

    @Test
    fun extractArchive_installsRequiredFilesAndRemovesStaging() {
        val archive = temporaryFolder.newFile("model.zip")
        writeValidArchive(archive)
        val models = temporaryFolder.newFolder("models")
        val store = VoskModelStore(models)
        val spec = specFor(archive)

        val result = store.extractArchive(archive, spec)

        assertTrue(result.isSuccess)
        val installed = result.getOrThrow()
        assertEquals(File(models, "test-model").canonicalPath, installed.canonicalPath)
        assertTrue(store.isInstalled(spec))
        assertFalse(File(models, ".staging-test-model").exists())
    }

    @Test
    fun extractArchive_acceptsOfficialRootDirDifferentFromInstallId() {
        val archive = temporaryFolder.newFile("official.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            listOf(
                "vosk-model-small-cn-0.22/am/final.mdl",
                "vosk-model-small-cn-0.22/conf/mfcc.conf",
            ).forEach { path ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(path.toByteArray())
                zip.closeEntry()
            }
        }
        val models = temporaryFolder.newFolder("models")
        val store = VoskModelStore(models)
        val spec = specFor(
            archive = archive,
            id = "vosk-small-cn-0.22",
            rootDirName = "vosk-model-small-cn-0.22",
        )

        val result = store.extractArchive(archive, spec)

        assertTrue(result.isSuccess)
        val installed = result.getOrThrow()
        assertEquals(File(models, "vosk-small-cn-0.22").canonicalPath, installed.canonicalPath)
        assertTrue(store.isInstalled(spec))
    }

    @Test
    fun extractArchive_rejectsZipSlip() {
        val archive = temporaryFolder.newFile("model.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("../escape.txt"))
            zip.write("bad".toByteArray())
            zip.closeEntry()
        }
        val models = temporaryFolder.newFolder("models")
        val store = VoskModelStore(models)
        val spec = specFor(archive)

        val result = store.extractArchive(archive, spec)

        assertTrue(result.isFailure)
        assertFalse(File(models.parentFile, "escape.txt").exists())
        assertFalse(File(models, ".staging-test-model").exists())
    }

    @Test
    fun inspect_reportsMissingRequiredFile() {
        val models = temporaryFolder.newFolder("models")
        val store = VoskModelStore(models)
        val spec = VoskModelSpec(
            id = "test-model",
            displayName = "测试模型",
            downloadUrl = "https://example.com/model.zip",
            archiveBytes = 1,
            sha256 = "0".repeat(64),
            rootDirName = "test-model",
            requiredPaths = listOf("am/final.mdl"),
        )
        val modelDir = File(models, "test-model").apply { mkdirs() }
        File(modelDir, "am").mkdirs()
        File(modelDir, "am/final.mdl").writeText("model")

        assertEquals(VoskModelState.Installed(modelDir.absolutePath), store.inspect(spec))

        File(modelDir, "am/final.mdl").delete()
        assertEquals(VoskModelState.NotInstalled, store.inspect(spec))
    }

    @Test
    fun inspect_detectsLegacyOfficialRootDir() {
        val models = temporaryFolder.newFolder("models")
        val store = VoskModelStore(models)
        val spec = VoskModelSpec(
            id = "vosk-small-cn-0.22",
            displayName = "测试模型",
            downloadUrl = "https://example.com/model.zip",
            archiveBytes = 1,
            sha256 = "0".repeat(64),
            rootDirName = "vosk-model-small-cn-0.22",
            requiredPaths = listOf("am/final.mdl", "conf/mfcc.conf"),
        )
        val legacyDir = File(models, "vosk-model-small-cn-0.22").apply { mkdirs() }
        File(legacyDir, "am").mkdirs()
        File(legacyDir, "am/final.mdl").writeText("model")
        File(legacyDir, "conf").mkdirs()
        File(legacyDir, "conf/mfcc.conf").writeText("config")

        assertEquals(VoskModelState.Installed(legacyDir.absolutePath), store.inspect(spec))
        assertTrue(store.isInstalled(spec))
    }

    @Test
    fun inspect_reportsArchiveReadyWhenFullPackageIsCached() {
        val models = temporaryFolder.newFolder("models")
        val store = VoskModelStore(models)
        val spec = VoskModelSpec(
            id = "test-model",
            displayName = "测试模型",
            downloadUrl = "https://example.com/model.zip",
            archiveBytes = 4,
            sha256 = "0".repeat(64),
            rootDirName = "test-model",
            requiredPaths = listOf("am/final.mdl"),
        )
        store.archivePart(spec).writeText("full")

        assertEquals(VoskModelState.ArchiveReady, store.inspect(spec))
    }

    private fun writeValidArchive(archive: File) {
        ZipOutputStream(archive.outputStream()).use { zip ->
            listOf(
                "test-model/am/final.mdl",
                "test-model/conf/mfcc.conf",
                "test-model/other.txt",
            ).forEach { path ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(path.toByteArray())
                zip.closeEntry()
            }
        }
    }
}
