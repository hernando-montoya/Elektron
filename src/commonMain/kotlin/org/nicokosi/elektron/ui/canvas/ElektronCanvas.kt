package org.nicokosi.elektron.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import org.nicokosi.elektron.domain.model.BoxElement
import org.nicokosi.elektron.domain.model.GraphicElement
import org.nicokosi.elektron.domain.model.SymbolElement
import org.nicokosi.elektron.domain.model.TextElement
import org.nicokosi.elektron.domain.model.WireElement
import org.nicokosi.elektron.viewmodel.CanvasState
import org.nicokosi.elektron.viewmodel.CanvasViewModel

// Colores de diseño del CAD
private val GridColorMinor = Color(0xFF23253A)
private val GridColorMajor = Color(0xFF333652)
private val OriginColor = Color(0xFF53587A)
private val SelectionColor = Color(0xFFFFB74D) // Ámbar de selección
private val PreviewColor = Color(0x9964B5F6)
private val TerminalSnapHaloColor = Color(0xFF81C784) // Verde esmeralda para imán de terminal
private val JunctionDotColor = Color(0xFF00E5FF) // Cyan brillante para uniones de cables

@Composable
fun ElektronCanvas(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    val canvasState by viewModel.state.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(if (canvasState.isWhiteBackground) Color(0xFFFFFFFF) else Color(0xFF161824))
            .onSizeChanged { size ->
                viewModel.updateViewportSize(size.toSize())
            }
            .pointerInput(canvasState.activeTool, canvasState.snapToGrid, canvasState.selectedSymbolTypeForPlacement) {
                detectDragGestures(
                    onDragStart = { startOffset ->
                        viewModel.onPointerDown(startOffset)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        viewModel.onPointerDrag(change.position, dragAmount)
                    },
                    onDragEnd = {
                        viewModel.onPointerUp()
                    },
                    onDragCancel = {
                        viewModel.onPointerUp()
                    }
                )
            }
            .pointerInput(Unit) {
                // Zoom dinámico con rueda de desplazamiento centrado en el cursor
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val scrollDelta = event.changes.firstOrNull()?.scrollDelta ?: continue
                            val position = event.changes.firstOrNull()?.position ?: continue

                            val zoomFactor = if (scrollDelta.y < 0) 1.15f else 0.85f
                            viewModel.zoom(zoomFactor, position)

                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        // Aplicamos la transformación del espacio de visualización (Pan & Zoom)
        withTransform({
            translate(left = canvasState.offset.x, top = canvasState.offset.y)
            scale(scaleX = canvasState.scale, scaleY = canvasState.scale, pivot = Offset.Zero)
        }) {
            // 1. Grilla
            if (canvasState.showGrid) {
                drawGrid(canvasState)
            }

            // 2. Origen (0,0)
            drawOriginCrosshair(canvasState.isWhiteBackground)

            // 3. Elementos guardados (Cajas, Símbolos, Cables, Textos)
            for (element in canvasState.elements) {
                drawGraphicElement(
                    drawScope = this,
                    element = element,
                    isPreview = false,
                    isWhiteBackground = canvasState.isWhiteBackground,
                    textMeasurer = textMeasurer
                )
            }

            // 4. Nodos de derivación / Uniones físicas reglamentarias
            val junctionDotColor = if (canvasState.isWhiteBackground) Color(0xFF0F172A) else JunctionDotColor
            val junctionCenterColor = if (canvasState.isWhiteBackground) Color(0xFFFFFFFF) else Color(0xFF161824)
            for (junction in canvasState.junctionPoints) {
                drawCircle(
                    color = junctionDotColor,
                    radius = 4.0f,
                    center = junction
                )
                drawCircle(
                    color = junctionCenterColor,
                    radius = 1.5f,
                    center = junction
                )
            }

            // 5. Elemento en construcción / previsualización
            canvasState.previewElement?.let { preview ->
                drawGraphicElement(
                    drawScope = this,
                    element = preview,
                    isPreview = true,
                    isWhiteBackground = canvasState.isWhiteBackground,
                    textMeasurer = textMeasurer
                )
            }

            // 6. Halo de imán a borna (Terminal Snapping Indicator)
            canvasState.hoveredTerminalPos?.let { termPos ->
                drawCircle(
                    color = TerminalSnapHaloColor.copy(alpha = 0.35f),
                    radius = 10f,
                    center = termPos
                )
                drawCircle(
                    color = TerminalSnapHaloColor,
                    radius = 5f,
                    center = termPos,
                    style = Stroke(width = 2.0f)
                )
            }
        }
    }
}

/**
 * Renderizado de primitivas gráficas y símbolos según su tipo.
 */
private fun drawGraphicElement(
    drawScope: DrawScope,
    element: GraphicElement,
    isPreview: Boolean = false,
    isWhiteBackground: Boolean = false,
    textMeasurer: TextMeasurer
) {
    when (element) {
        is WireElement -> {
            val color = if (element.isSelected) SelectionColor else if (isPreview) PreviewColor else element.color
            val width = if (element.isSelected) element.strokeWidth * 1.5f else element.strokeWidth

            // Renderizado ortogonal de todos los segmentos consecutivos
            for (i in 0 until element.points.size - 1) {
                val p1 = element.points[i]
                val p2 = element.points[i + 1]
                drawScope.drawLine(
                    color = color,
                    start = p1,
                    end = p2,
                    strokeWidth = width
                )
            }

            // Asas en los extremos
            drawScope.drawCircle(color = color, radius = width * 1.2f, center = element.startPoint)
            drawScope.drawCircle(color = color, radius = width * 1.2f, center = element.endPoint)

            if (element.isSelected) {
                for (p in element.points) {
                    drawScope.drawSelectionRing(p)
                }
            }
        }
        is BoxElement -> {
            val strokeColor = if (element.isSelected) SelectionColor else if (isPreview) PreviewColor else if (isWhiteBackground) Color(0xFF0288D1) else element.strokeColor
            val fillColor = if (isWhiteBackground) Color(0x180288D1) else element.fillColor
            val bounds = element.bounds()

            drawScope.drawRect(
                color = fillColor,
                topLeft = Offset(bounds.left, bounds.top),
                size = Size(bounds.width, bounds.height)
            )

            drawScope.drawRect(
                color = strokeColor,
                topLeft = Offset(bounds.left, bounds.top),
                size = Size(bounds.width, bounds.height),
                style = Stroke(
                    width = if (element.isSelected) element.strokeWidth * 1.5f else element.strokeWidth,
                    pathEffect = if (isPreview) PathEffect.dashPathEffect(floatArrayOf(8f, 6f)) else null
                )
            )

            if (element.isSelected) {
                drawScope.drawSelectionRing(Offset(bounds.left, bounds.top))
                drawScope.drawSelectionRing(Offset(bounds.right, bounds.bottom))
                drawScope.drawSelectionRing(Offset(bounds.left, bounds.bottom))
                drawScope.drawSelectionRing(Offset(bounds.right, bounds.top))
            }
        }
        is SymbolElement -> {
            SymbolRenderer.drawSymbol(
                drawScope = drawScope,
                symbol = element,
                isSelected = element.isSelected,
                isPreview = isPreview,
                isWhiteBackground = isWhiteBackground
            )

            if (element.isSelected) {
                val b = element.bounds()
                drawScope.drawRect(
                    color = SelectionColor,
                    topLeft = Offset(b.left - 4f, b.top - 4f),
                    size = Size(b.width + 8f, b.height + 8f),
                    style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)))
                )
            }
        }
        is TextElement -> {
            val textColor = when {
                element.isSelected -> SelectionColor
                element.color != Color.Unspecified -> element.color
                isWhiteBackground -> Color(0xFF0F172A)
                else -> Color(0xFFECEFF1)
            }

            drawScope.drawText(
                textMeasurer = textMeasurer,
                text = element.text,
                topLeft = element.position,
                style = TextStyle(
                    color = textColor,
                    fontSize = element.fontSize.sp,
                    fontWeight = if (element.isBold) FontWeight.Bold else FontWeight.Normal
                )
            )

            if (element.isSelected) {
                val b = element.bounds()
                drawScope.drawRect(
                    color = SelectionColor,
                    topLeft = Offset(b.left - 3f, b.top - 2f),
                    size = Size(b.width + 6f, b.height + 4f),
                    style = Stroke(
                        width = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawSelectionRing(center: Offset) {
    drawCircle(
        color = SelectionColor,
        radius = 5f,
        center = center,
        style = Stroke(width = 1.5f)
    )
}

/**
 * Dibuja la grilla adaptativa optimizada.
 */
private fun DrawScope.drawGrid(state: CanvasState) {
    val gridSize = state.gridSize
    val isWhite = state.isWhiteBackground
    val majorColor = if (isWhite) Color(0xFFCBD5E1) else GridColorMajor
    val minorColor = if (isWhite) Color(0xFFE2E8F0) else GridColorMinor

    val visibleLeft = -state.offset.x / state.scale - gridSize
    val visibleTop = -state.offset.y / state.scale - gridSize
    val visibleRight = (size.width - state.offset.x) / state.scale + gridSize
    val visibleBottom = (size.height - state.offset.y) / state.scale + gridSize

    val startX = (visibleLeft / gridSize).toInt() * gridSize
    val startY = (visibleTop / gridSize).toInt() * gridSize

    val minorStrokeWidth = 0.6f / state.scale.coerceAtLeast(0.5f)
    val majorStrokeWidth = 1.2f / state.scale.coerceAtLeast(0.5f)

    var x = startX
    while (x <= visibleRight) {
        val gridIndex = (x / gridSize).toInt()
        val isMajor = gridIndex % 5 == 0
        drawLine(
            color = if (isMajor) majorColor else minorColor,
            start = Offset(x, visibleTop),
            end = Offset(x, visibleBottom),
            strokeWidth = if (isMajor) majorStrokeWidth else minorStrokeWidth
        )
        x += gridSize
    }

    var y = startY
    while (y <= visibleBottom) {
        val gridIndex = (y / gridSize).toInt()
        val isMajor = gridIndex % 5 == 0
        drawLine(
            color = if (isMajor) majorColor else minorColor,
            start = Offset(visibleLeft, y),
            end = Offset(visibleRight, y),
            strokeWidth = if (isMajor) majorStrokeWidth else minorStrokeWidth
        )
        y += gridSize
    }
}

/**
 * Marca de referencia de origen (0,0).
 */
private fun DrawScope.drawOriginCrosshair(isWhiteBackground: Boolean = false) {
    val crosshairSize = 24f
    val color = if (isWhiteBackground) Color(0xFF94A3B8) else OriginColor
    drawLine(
        color = color,
        start = Offset(-crosshairSize, 0f),
        end = Offset(crosshairSize, 0f),
        strokeWidth = 2f
    )
    drawLine(
        color = color,
        start = Offset(0f, -crosshairSize),
        end = Offset(0f, crosshairSize),
        strokeWidth = 2f
    )
    drawCircle(
        color = color,
        radius = 3.5f,
        center = Offset.Zero
    )
}
