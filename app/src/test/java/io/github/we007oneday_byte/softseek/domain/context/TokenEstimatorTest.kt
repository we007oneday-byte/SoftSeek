package io.github.we007oneday_byte.softseek.domain.context

import org.junit.Assert.assertEquals
import org.junit.Test

class TokenEstimatorTest {
    private val estimator = TokenEstimator()

    @Test
    fun estimatesTextAndImages() {
        assertEquals(4, estimator.estimateText("123456", 1.5))
        assertEquals(768, estimator.estimateImages(2))
        assertEquals(772, estimator.estimateMessage("123456", 2))
    }

    @Test
    fun truncatesByCharacterBudget() {
        assertEquals("abc", estimator.truncateText("abcdefghij", maxTokens = 2, tokenEstimateRatio = 1.5))
        assertEquals("", estimator.truncateText("abcdefghij", maxTokens = 0))
    }

    @Test
    fun calibratesRatioFromUsage() {
        assertEquals(1.25, estimator.calibrateRatio(1.5, 10, 12), 0.001)
        assertEquals(1.5, estimator.calibrateRatio(1.5, 0, 12), 0.001)
        assertEquals(6.0, estimator.calibrateRatio(1.5, 10, 1), 0.001)
    }
}
