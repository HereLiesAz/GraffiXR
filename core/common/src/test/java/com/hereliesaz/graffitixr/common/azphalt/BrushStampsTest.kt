package com.hereliesaz.graffitixr.common.azphalt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrushStampsTest {

    @Test
    fun emptyInputYieldsNoDabs() {
        assertTrue(BrushStamps.place(emptyList(), 5f).isEmpty())
    }

    @Test
    fun singlePointYieldsOneDab() {
        assertEquals(listOf(3f, 4f), BrushStamps.place(listOf(3f, 4f), 5f))
    }

    @Test
    fun evenlySpacesDabsAlongAStraightLine() {
        // 0..10 on X, step 2 → dabs at x = 0,2,4,6,8,10.
        val dabs = BrushStamps.place(listOf(0f, 0f, 10f, 0f), stepPx = 2f)
        assertEquals(listOf(0f, 0f, 2f, 0f, 4f, 0f, 6f, 0f, 8f, 0f, 10f, 0f), dabs)
    }

    @Test
    fun spacingIsMeasuredAlongArcLengthAcrossSegments() {
        // Right 3 then up 4 = an L of total length 7; step 5 → dab at start, at 5 (2 up the vertical
        // leg → (3,2)), and none at 10 (past the end).
        val dabs = BrushStamps.place(listOf(0f, 0f, 3f, 0f, 3f, 4f), stepPx = 5f)
        assertEquals(listOf(0f, 0f, 3f, 2f), dabs)
    }

    @Test
    fun zeroLengthSegmentsAreSkippedNotDuplicated() {
        // A repeated point in the middle must not spawn a dab nor stall the walk.
        val dabs = BrushStamps.place(listOf(0f, 0f, 5f, 0f, 5f, 0f, 10f, 0f), stepPx = 5f)
        assertEquals(listOf(0f, 0f, 5f, 0f, 10f, 0f), dabs)
    }

    @Test
    fun nonPositiveStepDoesNotHang() {
        // Guarded to a tiny step — finite output, first dab still on the start.
        val dabs = BrushStamps.place(listOf(0f, 0f, 0.05f, 0f), stepPx = 0f)
        assertTrue(dabs.size >= 2)
        assertEquals(0f, dabs[0], 0f)
        assertEquals(0f, dabs[1], 0f)
    }

    @Test
    fun lengthSumsSegmentDistances() {
        assertEquals(7f, BrushStamps.length(listOf(0f, 0f, 3f, 0f, 3f, 4f)), 1e-4f)
        assertEquals(0f, BrushStamps.length(listOf(2f, 2f)), 0f)
    }
}
