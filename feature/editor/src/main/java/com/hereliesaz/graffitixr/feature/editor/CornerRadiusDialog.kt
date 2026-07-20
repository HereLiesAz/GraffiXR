// FILE: feature/editor/src/main/java/com/hereliesaz/graffitixr/feature/editor/CornerRadiusDialog.kt
package com.hereliesaz.graffitixr.feature.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.hereliesaz.aznavrail.AzButton
import com.hereliesaz.aznavrail.model.AzButtonShape
import kotlin.math.roundToInt

/**
 * Sets the corner radius for the active vector layer's rectangle shapes. Seeded from
 * [currentRadius]; [onApply] pushes the chosen radius to the view model, which clamps it per-shape
 * to half the shorter side (so a large value simply pill-rounds the rectangle).
 */
@Composable
fun CornerRadiusDialog(
    currentRadius: Float,
    onApply: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    var radius by remember { mutableFloatStateOf(currentRadius.coerceIn(0f, 200f)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Corner radius") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(if (radius < 0.5f) "Square corners" else "${radius.roundToInt()} px")
                Slider(
                    value = radius,
                    onValueChange = { radius = it },
                    valueRange = 0f..200f,
                )
            }
        },
        confirmButton = {
            AzButton(
                text = "Apply",
                onClick = { onApply(radius) },
                shape = AzButtonShape.RECTANGLE,
            )
        },
        dismissButton = {
            AzButton(text = "Cancel", onClick = onDismiss, shape = AzButtonShape.RECTANGLE)
        },
    )
}
