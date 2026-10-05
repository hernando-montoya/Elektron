package org.nicokosi.elektron.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Referencia a una conexión formal entre un cable y una borna de un símbolo.
 */
data class TerminalAnchor(
    val elementId: String,
    val terminalId: String
)

/**
 * Representa cualquier elemento gráfico dibujado en el esquema eléctrico.
 */
sealed interface GraphicElement {
    val id: String
    val isSelected: Boolean

    fun copyWithSelected(selected: Boolean): GraphicElement
    fun bounds(): Rect
    fun hits(point: Offset, tolerance: Float = 6f): Boolean
    fun translate(delta: Offset): GraphicElement
}

/**
 * Texto libre o anotación técnica en el plano (ej. "SCHÉMA DÉVELOPPÉ", "B.R.", "B.D.").
 */
data class TextElement(
    override val id: String,
    val text: String,
    val position: Offset,
    val fontSize: Float = 13f, // 10f, 13f, 18f
    val isBold: Boolean = false,
    val color: Color = Color.Unspecified, // Si es Unspecified toma el color de texto del tema
    override val isSelected: Boolean = false
) : GraphicElement {

    override fun copyWithSelected(selected: Boolean): GraphicElement =
        copy(isSelected = selected)

    override fun bounds(): Rect {
        val estimatedWidth = text.length * fontSize * 0.65f
        val height = fontSize * 1.3f
        return Rect(
            left = position.x,
            top = position.y,
            right = position.x + max(estimatedWidth, 20f),
            bottom = position.y + height
        )
    }

    override fun hits(point: Offset, tolerance: Float): Boolean {
        val b = bounds()
        return point.x in (b.left - tolerance)..(b.right + tolerance) &&
               point.y in (b.top - tolerance)..(b.bottom + tolerance)
    }

    override fun translate(delta: Offset): GraphicElement =
        copy(position = position + delta)
}

/**
 * Conductor o cable eléctrico normalizado.
 */
data class WireElement(
    override val id: String,
    val points: List<Offset>,
    val strokeWidth: Float = 2.5f,
    val color: Color = Color(0xFF64B5F6),
    val startAnchor: TerminalAnchor? = null,
    val endAnchor: TerminalAnchor? = null,
    override val isSelected: Boolean = false
) : GraphicElement {

    constructor(
        id: String,
        start: Offset,
        end: Offset,
        strokeWidth: Float = 2.5f,
        color: Color = Color(0xFF64B5F6),
        startAnchor: TerminalAnchor? = null,
        endAnchor: TerminalAnchor? = null,
        isSelected: Boolean = false
    ) : this(
        id = id,
        points = generateOrthogonalPath(start, end),
        strokeWidth = strokeWidth,
        color = color,
        startAnchor = startAnchor,
        endAnchor = endAnchor,
        isSelected = isSelected
    )

    val startPoint: Offset get() = points.firstOrNull() ?: Offset.Zero
    val endPoint: Offset get() = points.lastOrNull() ?: Offset.Zero

    override fun copyWithSelected(selected: Boolean): GraphicElement =
        copy(isSelected = selected)

    override fun bounds(): Rect {
        if (points.isEmpty()) return Rect.Zero
        var minX = points[0].x
        var minY = points[0].y
        var maxX = points[0].x
        var maxY = points[0].y

        for (p in points) {
            minX = min(minX, p.x)
            minY = min(minY, p.y)
            maxX = max(maxX, p.x)
            maxY = max(maxY, p.y)
        }
        return Rect(minX - strokeWidth, minY - strokeWidth, maxX + strokeWidth, maxY + strokeWidth)
    }

    override fun hits(point: Offset, tolerance: Float): Boolean {
        if (points.size < 2) return false
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            if (pointNearSegment(point, p1, p2, tolerance + strokeWidth / 2)) {
                return true
            }
        }
        return false
    }

    override fun translate(delta: Offset): GraphicElement =
        copy(points = points.map { it + delta })

    companion object {
        fun generateOrthogonalPath(start: Offset, end: Offset): List<Offset> {
            if (start == end) return listOf(start, end)
            if (abs(start.x - end.x) < 1f || abs(start.y - end.y) < 1f) {
                return listOf(start, end)
            }
            val corner = Offset(end.x, start.y)
            return listOf(start, corner, end)
        }

        private fun pointNearSegment(point: Offset, start: Offset, end: Offset, tolerance: Float): Boolean {
            val l2 = (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
            if (l2 == 0f) return (point - start).getDistance() <= tolerance
            val t = ((point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)) / l2
            val clampedT = t.coerceIn(0f, 1f)
            val projection = Offset(
                start.x + clampedT * (end.x - start.x),
                start.y + clampedT * (end.y - start.y)
            )
            return (point - projection).getDistance() <= tolerance
        }
    }
}

/**
 * Caja de empalme, envolvente o cuadro general de distribución (TGBT).
 */
data class BoxElement(
    override val id: String,
    val topLeft: Offset,
    val bottomRight: Offset,
    val strokeWidth: Float = 2.0f,
    val strokeColor: Color = Color(0xFFE0E0E0),
    val fillColor: Color = Color(0x2264B5F6),
    override val isSelected: Boolean = false
) : GraphicElement {

    override fun copyWithSelected(selected: Boolean): GraphicElement =
        copy(isSelected = selected)

    override fun bounds(): Rect {
        val minX = min(topLeft.x, bottomRight.x)
        val minY = min(topLeft.y, bottomRight.y)
        val maxX = max(topLeft.x, bottomRight.x)
        val maxY = max(topLeft.y, bottomRight.y)
        return Rect(minX, minY, maxX, maxY)
    }

    override fun hits(point: Offset, tolerance: Float): Boolean {
        val b = bounds()
        return point.x in (b.left - tolerance)..(b.right + tolerance) &&
               point.y in (b.top - tolerance)..(b.bottom + tolerance)
    }

    override fun translate(delta: Offset): GraphicElement =
        copy(topLeft = topLeft + delta, bottomRight = bottomRight + delta)
}
