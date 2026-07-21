// FILE: core/common/src/main/java/com/hereliesaz/graffitixr/common/model/PathSimplify.kt
package com.hereliesaz.graffitixr.common.model

import kotlin.math.hypot

/**
 * Ramer–Douglas–Peucker poly-line simplification — drops points that lie within [epsilon] of the line
 * they'd otherwise bend, keeping the overall shape. Used to clean a freeform pen stroke (which
 * captures a point per touch event) into a tidy vector path. Pure and unit-tested; points are
 * interleaved `[x0,y0,x1,y1,…]`.
 */
object PathSimplify {

    /** Returns a simplified copy of [points]; unchanged if fewer than 3 points or [epsilon] <= 0. */
    fun rdp(points: List<Float>, epsilon: Float): List<Float> {
        val n = points.size / 2
        if (n < 3 || epsilon <= 0f) return points
        val keep = BooleanArray(n)
        keep[0] = true
        keep[n - 1] = true
        simplify(points, 0, n - 1, epsilon, keep)
        val out = ArrayList<Float>(points.size)
        for (i in 0 until n) if (keep[i]) { out.add(points[2 * i]); out.add(points[2 * i + 1]) }
        return out
    }

    private fun simplify(p: List<Float>, start: Int, end: Int, eps: Float, keep: BooleanArray) {
        if (end <= start + 1) return
        val ax = p[2 * start]; val ay = p[2 * start + 1]
        val bx = p[2 * end]; val by = p[2 * end + 1]
        var maxDist = -1f
        var idx = -1
        for (i in start + 1 until end) {
            val d = perpDistance(p[2 * i], p[2 * i + 1], ax, ay, bx, by)
            if (d > maxDist) { maxDist = d; idx = i }
        }
        if (maxDist > eps && idx >= 0) {
            keep[idx] = true
            simplify(p, start, idx, eps, keep)
            simplify(p, idx, end, eps, keep)
        }
    }

    /** Perpendicular distance from ([px],[py]) to the infinite line through ([ax],[ay])–([bx],[by]). */
    private fun perpDistance(px: Float, py: Float, ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        if (len2 == 0f) return hypot(px - ax, py - ay)
        val t = ((px - ax) * dx + (py - ay) * dy) / len2
        return hypot(px - (ax + t * dx), py - (ay + t * dy))
    }
}
