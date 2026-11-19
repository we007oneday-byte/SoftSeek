package io.github.we007oneday_byte.softseek.data.local

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class DatabaseSchemaTest {
    @Test
    fun exportedSchemaContainsFourTablesAndRequiredConstraints() {
        val schemaFile = File(
            System.getProperty("user.dir"),
            "schemas/io.github.we007oneday_byte.softseek.data.local.AppDatabase/1.json",
        )
        assertTrue("schema export missing: ${schemaFile.absolutePath}", schemaFile.exists())

        val database = JSONObject(schemaFile.readText()).getJSONObject("database")
        assertEquals(1, database.getInt("version"))
        val entities = database.getJSONArray("entities")
        val tables = (0 until entities.length()).associate { index ->
            val entity = entities.getJSONObject(index)
            entity.getString("tableName") to entity
        }
        assertEquals(
            setOf(
                "memory_pool_blocks",
                "memory_heap_nodes",
                "message_attachments",
                "memory_pool_config",
            ),
            tables.keys,
        )

        val blockIndices = tables.getValue("memory_pool_blocks").getJSONArray("indices")
        val blockIndexNames = (0 until blockIndices.length())
            .map { blockIndices.getJSONObject(it).getString("name") }
            .toSet()
        assertTrue(blockIndexNames.contains("index_memory_pool_blocks_isDeleted_updatedAt"))
        assertTrue(blockIndexNames.contains("index_memory_pool_blocks_isPinned_updatedAt"))

        val nodeCreateSql = tables.getValue("memory_heap_nodes").getString("createSql")
        assertTrue(nodeCreateSql.contains("ON DELETE CASCADE"))
        val attachmentForeignKeys = tables.getValue("message_attachments").getJSONArray("foreignKeys")
        assertEquals(
            "memory_heap_nodes",
            attachmentForeignKeys.getJSONObject(0).getString("table"),
        )
    }

    @Test
    fun databaseSurvivesColdReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "schema-reopen-${UUID.randomUUID()}.db"
        context.deleteDatabase(databaseName)

        var database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        database.memoryPoolBlockDao().upsert(
            io.github.we007oneday_byte.softseek.data.local.entity.MemoryPoolBlockEntity(
                poolId = "pool-1",
                title = "冷启动",
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        database.close()

        database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName).build()
        val block = database.memoryPoolBlockDao().getById("pool-1")
        assertNotNull(block)
        assertEquals("冷启动", block?.title)
        database.close()
        context.deleteDatabase(databaseName)
    }
}
