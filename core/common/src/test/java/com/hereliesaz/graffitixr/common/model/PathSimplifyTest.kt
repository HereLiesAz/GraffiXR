// FILE: core/common/src/test/java/com/hereliesaz/graffitixr/common/model/PathSimplifyTest.kt
package com.hereliesaz.graffitixr.common.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PathSimplifyTest {

    @Test
    fun dropsNearCollinearPoints() {
        // Midpoint is only 0.1 off the straight line → removed at epsilon 1.
        val out = PathSimplify.rdp(listOf(0f, 0f, 5f, 0.1f, 10f, 0f), epsilon = 1f)
        assertEquals(listOf(0f, 0f, 10f, 0f), out)
    }

    @Test
    fun keepsPointsThatBendBeyondEpsilon() {
        // Midpoint 5 off the line → kept.
        val out = PathSimplify.rdp(listOf(0f, 0f, 5f, 5f, 10f, 0f), epsilon = 1f)
        assertEquals(listOf(0f, 0f, 5f, 5f, 10f, 0f), out)
    }

    @Test
    fun keepsEndpointsAndReducesDenseLine() {
        val dense = (0..10).flatMap { listOf(it.toFloat(), 0f) } // 11 collinear points
        val out = PathSimplify.rdp(dense, epsilon = 0.5f)
        assertEquals(listOf(0f, 0f, 10f, 0f), out)
    }

    @Test
    fun returnsInputWhenTooFewPointsOrNoEpsilon() {
        val two = listOf(0f, 0f, 3f, 4f)
        assertSame(two, PathSimplify.rdp(two, 1f))
        val many = listOf(0f, 0f, 5f, 5f, 10f, 0f)
        assertSame(many, PathSimplify.rdp(many, 0f))
    }
}
