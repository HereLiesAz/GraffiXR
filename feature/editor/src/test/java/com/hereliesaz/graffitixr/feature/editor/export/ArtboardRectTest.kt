// FILE: feature/editor/src/test/java/com/hereliesaz/graffitixr/feature/editor/export/ArtboardRectTest.kt
package com.hereliesaz.graffitixr.feature.editor.export

import org.junit.Assert.assertArrayEquals
import org.junit.Test

/** Geometry checks for [artboardRect] — the centred aspect-fit of the document within the canvas. */
class ArtboardRectTest {

    @Test
    fun squareDocInLandscapeCanvas_isPillarboxed() {
        // 2000x1000 canvas, 1000x1000 doc → 1000x1000 artboard centred horizontally.
        assertArrayEquals(
            floatArrayOf(500f, 0f, 1000f, 1000f),
            artboardRect(2000, 1000, 1000, 1000), 1e-3f,
        )
    }

    @Test
    fun wideDocInTallCanvas_isLetterboxed() {
        // 1000x2000 canvas, 1000x500 doc (aspect 2) → full width, 500 tall, centred vertically.
        assertArrayEquals(
            floatArrayOf(0f, 750f, 1000f, 500f),
            artboardRect(1000, 2000, 1000, 500), 1e-3f,
        )
    }

    @Test
    fun matchingAspect_fillsCanvas() {
        assertArrayEquals(
            floatArrayOf(0f, 0f, 1000f, 1000f),
            artboardRect(1000, 1000, 500, 500), 1e-3f,
        )
    }

    @Test
    fun degenerateInput_fallsBackToWholeCanvas() {
        assertArrayEquals(
            floatArrayOf(0f, 0f, 800f, 600f),
            artboardRect(800, 600, 0, 600), 1e-3f,
        )
    }
}
