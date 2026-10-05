package org.nicokosi.elektron.domain.geometry

import androidx.compose.ui.geometry.Offset
import kotlin.math.round

/**
 * Utilidades geométricas para snapping a grilla y transformaciones.
 */
object SnapUtils {
    /**
     * Ajusta un punto en coordenadas de mundo al punto de grilla más cercano.
     */
    fun snapToGrid(point: Offset, gridSize: Float): Offset {
        if (gridSize <= 0f) return point
        val snappedX = round(point.x / gridSize) * gridSize
        val snappedY = round(point.y / gridSize) * gridSize
        return Offset(snappedX, snappedY)
    }

    /**
     * Transforma una coordenada de pantalla (viewport) a coordenadas de mundo.
     */
    fun screenToWorld(screenPoint: Offset, panOffset: Offset, scale: Float): Offset {
        return (screenPoint - panOffset) / scale
    }

    /**
     * Transforma una coordenada de mundo a coordenada de pantalla (viewport).
     */
    fun worldToScreen(worldPoint: Offset, panOffset: Offset, scale: Float): Offset {
        return (worldPoint * scale) + panOffset
    }
}
