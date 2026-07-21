package com.hereliesaz.graffitixr.common.azphalt

import kotlin.math.hypot

/**
 * The stamp-spacing core of a raster brush: turns a recorded stroke poly-line into the ordered list of
 * dab centres a stamp brush lays down. This is the "how does Procreate do it" heart of a stroke — the
 * tip is stamped every fixed arc-length step regardless of how fast or coarsely the finger moved, so a
 * quick flick and a slow drag paint the same density.
 *
 * Pure and Android-free (interleaved `[x0,y0,x1,y1,…]`, same convention as [com.hereliesaz.graffitixr
 * .common.model.PathSimplify]) so the spacing maths is unit-tested; the editor's Android renderer takes
 * these centres, applies per-dab jitter/scatter, and rasterizes the stamp at each.
 */
object BrushStamps {

    /**
     * Resample [points] into dab centres [stepPx] apart along the stroke's arc length. The first point
     * is always emitted; subsequent dabs are interpolated at each multiple of [stepPx] of travelled
     * distance, so coverage is even independent of the input's point density. Degenerate input (fewer
     * than 2 points) returns the single point (or empty); a non-positive [stepPx] is treated as a tiny
     * step so callers never divide by zero.
     *
     * @param stepPx dab spacing in the SAME units as [points] — i.e. `brush.spacing * diameterPx`.
     */
    fun place(points: List<Float>, stepPx: Float): List<Float> {
        val n = points.size / 2
        if (n == 0) return emptyList()
        if (n == 1) return listOf(points[0], points[1])
        val step = if (stepPx > 0f) stepPx else 0.01f

        val out = ArrayList<Float>()
        // First dab sits on the stroke start.
        out.add(points[0]); out.add(points[1])

        var nextAt = step          // arc-length at which the next dab is due
        var travelled = 0f         // arc-length consumed up to the start of the current segment
        for (i in 0 until n - 1) {
            val ax = points[2 * i]; val ay = points[2 * i + 1]
            val bx = points[2 * i + 2]; val by = points[2 * i + 3]
            val segLen = hypot(bx - ax, by - ay)
            if (segLen == 0f) continue
            // Emit every dab whose arc-length target falls within this segment.
            while (nextAt <= travelled + segLen) {
                val t = (nextAt - travelled) / segLen
                out.add(ax + (bx - ax) * t)
                out.add(ay + (by - ay) * t)
                nextAt += step
            }
            travelled += segLen
        }
        return out
    }

    /** Total arc length of a poly-line — the stroke length a caller divides by [stepPx] for a dab count. */
    fun length(points: List<Float>): Float {
        val n = points.size / 2
        var total = 0f
        for (i in 0 until n - 1) {
            total += hypot(points[2 * i + 2] - points[2 * i], points[2 * i + 3] - points[2 * i + 1])
        }
        return total
    }
}
