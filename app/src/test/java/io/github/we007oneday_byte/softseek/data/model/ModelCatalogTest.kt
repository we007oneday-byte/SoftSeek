package io.github.we007oneday_byte.softseek.data.model

import io.github.we007oneday_byte.softseek.domain.model.AiChatRequest
import io.github.we007oneday_byte.softseek.domain.model.AiModelInfo
import io.github.we007oneday_byte.softseek.domain.model.BalanceInfo
import io.github.we007oneday_byte.softseek.domain.model.ChatStreamEvent
import io.github.we007oneday_byte.softseek.domain.model.ProviderCapabilities
import io.github.we007oneday_byte.softseek.domain.provider.AiProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun ensureModelAddsManualModelToCatalog() {
        val catalog = ModelCatalog(FakeProvider())

        val manual = catalog.ensureModel("gpt-4o-mini", maxOutputTokens = 8192)

        assertEquals("gpt-4o-mini", manual.id)
        assertTrue(catalog.contains("gpt-4o-mini"))
        assertEquals(8192, catalog.model("gpt-4o-mini")?.maxOutputTokens)
        assertTrue(catalog.supportsVision("gpt-4o-mini"))
    }

    @Test
    fun refreshUsesRemoteModelsAndMarksLoaded() = runTest {
        val catalog = ModelCatalog(FakeProvider(models = listOf(model("remote-model"))))

        val result = catalog.refresh("sk-test")

        assertTrue(result.isSuccess)
        assertEquals(listOf("remote-model"), catalog.models.value.map { it.id })
        assertTrue(catalog.hasRemoteModels.value)
    }

    @Test
    fun refreshFailureKeepsExistingCatalog() = runTest {
        val catalog = ModelCatalog(FakeProvider(failListModels = true))

        val result = catalog.refresh("sk-test")

        assertTrue(result.isFailure)
        assertTrue(catalog.contains("deepseek-chat"))
        assertFalse(catalog.hasRemoteModels.value)
    }

    @Test
    fun refreshWithoutKeyDoesNotCallProvider() = runTest {
        val catalog = ModelCatalog(FakeProvider(models = listOf(model("remote-model"))))

        catalog.refresh(null)

        assertFalse(catalog.contains("remote-model"))
        assertNull(catalog.model("remote-model"))
    }

    @Test
    fun resetForProviderUsesProviderDefaultsAndClearsRemoteFlag() = runTest {
        val catalog = ModelCatalog(
            FakeProvider(
                models = listOf(model("remote-model")),
                defaults = listOf(model("provider-default")),
            ),
        )
        catalog.refresh("sk-test")
        assertTrue(catalog.hasRemoteModels.value)

        catalog.resetForProvider()

        assertEquals(listOf("provider-default"), catalog.models.value.map { it.id })
        assertFalse(catalog.hasRemoteModels.value)
    }

    private fun model(id: String) = AiModelInfo(
        id = id,
        displayName = id,
        contextWindow = 0,
        maxOutputTokens = 0,
        inputModalities = listOf("text"),
        reasoningLevels = emptyList(),
        defaultReasoningEffort = null,
    )

    private class FakeProvider(
        private val models: List<AiModelInfo> = emptyList(),
        private val failListModels: Boolean = false,
        private val defaults: List<AiModelInfo> = emptyList(),
    ) : AiProvider {
        override val id: String = "fake"
        override val displayName: String = "Fake"
        override val capabilities: ProviderCapabilities = ProviderCapabilities()

        override fun streamChat(apiKey: String, request: AiChatRequest): Flow<ChatStreamEvent> = emptyFlow()

        override suspend fun listModels(apiKey: String): List<AiModelInfo> {
            if (failListModels) error("list models failed")
            return models
        }

        override suspend fun getBalance(apiKey: String): BalanceInfo? = null

        override fun defaultModels(): List<AiModelInfo> = defaults
    }
}
