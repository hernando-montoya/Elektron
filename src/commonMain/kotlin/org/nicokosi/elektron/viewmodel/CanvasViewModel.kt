package org.nicokosi.elektron.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.nicokosi.elektron.domain.export.ProjectDocument
import org.nicokosi.elektron.domain.export.ProjectMetadata
import org.nicokosi.elektron.domain.export.ProjectSerializer
import org.nicokosi.elektron.domain.export.SvgExporter
import org.nicokosi.elektron.domain.geometry.SnapUtils
import org.nicokosi.elektron.domain.model.*
import org.nicokosi.elektron.domain.validation.ComplianceReport
import org.nicokosi.elektron.domain.validation.NFC15100Validator

/**
 * Herramientas de interacción disponibles en el CAD.
 */
enum class Tool {
    SELECT,
    PAN,
    WIRE,
    BOX,
    PLACE_SYMBOL,
    TEXT
}

/**
 * Estado inmutable del lienzo de dibujo.
 */
data class CanvasState(
    val offset: Offset = Offset.Zero,
    val scale: Float = 1.0f,
    val gridSize: Float = 20f,
    val showGrid: Boolean = true,
    val snapToGrid: Boolean = true,
    val isWhiteBackground: Boolean = false,
    val showSidebar: Boolean = true,
    val showProperties: Boolean = true,
    val showBomModal: Boolean = false,
    val showShortcutsModal: Boolean = false,
    val activeTool: Tool = Tool.SELECT,
    val activeWireColor: Color = Color(0xFFE53935), // Rojo de Fase por defecto
    val selectedSymbolTypeForPlacement: SymbolType? = null,
    val elements: List<GraphicElement> = emptyList(),
    val previewElement: GraphicElement? = null,
    val hoveredTerminalPos: Offset? = null,
    val junctionPoints: List<Offset> = emptyList(),
    val netsCount: Int = 0,
    val complianceReport: ComplianceReport = ComplianceReport(emptyList(), 0, 0),
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val projectTitle: String = "Installation NF C 15-100",
    val statusNotification: String? = null,
    val viewportSize: Size = Size.Zero
) {
    companion object {
        const val MIN_SCALE = 0.1f
        const val MAX_SCALE = 5.0f
    }
}

/**
 * ViewModel centralizado con StateFlow y patrón Command/Snapshot para Undo/Redo.
 */
class CanvasViewModel {

    private val _state = MutableStateFlow(CanvasState())
    val state: StateFlow<CanvasState> = _state.asStateFlow()

    // Historial para Undo / Redo
    private val undoStack = mutableListOf<List<GraphicElement>>()
    private val redoStack = mutableListOf<List<GraphicElement>>()

    // Variables transitorias de dibujo e interacción
    private var dragStartWorldPoint: Offset? = null
    private var startTerminalAnchor: TerminalAnchor? = null
    private var isDraggingElements: Boolean = false
    private var idCounter = 1L

    private fun nextId(prefix: String): String = "$prefix-${idCounter++}"

    fun updateViewportSize(size: Size) {
        _state.update { it.copy(viewportSize = size) }
    }

    fun showNotification(message: String) {
        _state.update { it.copy(statusNotification = message) }
    }

    fun clearNotification() {
        _state.update { it.copy(statusNotification = null) }
    }

    fun toggleBomModal(show: Boolean) {
        _state.update { it.copy(showBomModal = show) }
    }

    fun toggleShortcutsModal(show: Boolean) {
        _state.update { it.copy(showShortcutsModal = show) }
    }

    fun toggleProperties() {
        _state.update { it.copy(showProperties = !it.showProperties) }
    }

    fun pan(delta: Offset) {
        _state.update { current ->
            current.copy(offset = current.offset + delta)
        }
    }

    fun zoom(factor: Float, centroid: Offset? = null) {
        _state.update { current ->
            val center = centroid ?: if (current.viewportSize != Size.Zero) {
                Offset(current.viewportSize.width / 2f, current.viewportSize.height / 2f)
            } else {
                Offset.Zero
            }

            val newScale = (current.scale * factor).coerceIn(
                CanvasState.MIN_SCALE,
                CanvasState.MAX_SCALE
            )
            val actualFactor = newScale / current.scale
            val newOffset = center - (center - current.offset) * actualFactor

            current.copy(scale = newScale, offset = newOffset)
        }
    }

    fun setTool(tool: Tool) {
        _state.update {
            it.copy(
                activeTool = tool,
                previewElement = null,
                hoveredTerminalPos = null,
                selectedSymbolTypeForPlacement = if (tool == Tool.PLACE_SYMBOL) it.selectedSymbolTypeForPlacement else null
            )
        }
        dragStartWorldPoint = null
        startTerminalAnchor = null
    }

    fun selectSymbolForPlacement(type: SymbolType) {
        _state.update {
            it.copy(
                activeTool = Tool.PLACE_SYMBOL,
                selectedSymbolTypeForPlacement = type,
                previewElement = null,
                hoveredTerminalPos = null
            )
        }
    }

    fun toggleSidebar() {
        _state.update { it.copy(showSidebar = !it.showSidebar) }
    }

    fun toggleGrid() {
        _state.update { it.copy(showGrid = !it.showGrid) }
    }

    fun toggleSnap() {
        _state.update { it.copy(snapToGrid = !it.snapToGrid) }
    }

    fun toggleWhiteBackground() {
        _state.update { it.copy(isWhiteBackground = !it.isWhiteBackground) }
    }

    fun resetView() {
        _state.update { it.copy(offset = Offset.Zero, scale = 1.0f) }
    }

    fun setGridSize(size: Float) {
        _state.update { it.copy(gridSize = size.coerceIn(5f, 100f)) }
    }

    fun setActiveWireColor(color: Color) {
        _state.update { it.copy(activeWireColor = color) }
    }

    fun rotateSelected() {
        val currentElements = _state.value.elements
        val hasSelection = currentElements.any { it.isSelected && it is SymbolElement }
        if (hasSelection) {
            saveSnapshot()
            val updated = currentElements.map { el ->
                if (el.isSelected && el is SymbolElement) {
                    el.rotateClockwise()
                } else {
                    el
                }
            }
            recalculateGraph(updated)
        }
    }

    fun updateSelectedText(
        text: String? = null,
        fontSize: Float? = null,
        isBold: Boolean? = null,
        color: Color? = null
    ) {
        saveSnapshot()
        val currentElements = _state.value.elements
        val updated = currentElements.map { el ->
            if (el.isSelected && el is TextElement) {
                el.copy(
                    text = text ?: el.text,
                    fontSize = fontSize ?: el.fontSize,
                    isBold = isBold ?: el.isBold,
                    color = color ?: el.color
                )
            } else {
                el
            }
        }
        recalculateGraph(updated)
    }

    fun updateSelectedSymbolProperties(label: String? = null, designation: String? = null) {
        val currentElements = _state.value.elements
        val updated = currentElements.map { el ->
            if (el.isSelected && el is SymbolElement) {
                el.copy(
                    label = label ?: el.label,
                    designation = designation ?: el.designation
                )
            } else {
                el
            }
        }
        recalculateGraph(updated)
    }

    fun updateSelectedWireColor(color: Color) {
        saveSnapshot()
        val currentElements = _state.value.elements
        val updated = currentElements.map { el ->
            if (el.isSelected && el is WireElement) {
                el.copy(color = color)
            } else {
                el
            }
        }
        recalculateGraph(updated)
    }

    fun selectElementsById(ids: List<String>) {
        if (ids.isEmpty()) return
        val currentElements = _state.value.elements
        val updated = currentElements.map { el ->
            el.copyWithSelected(el.id in ids)
        }
        _state.update { it.copy(elements = updated, activeTool = Tool.SELECT) }
    }

    // ==========================================
    // GESTIÓN DE ATAJOS DE TECLADO
    // ==========================================

    fun handleKeyDown(key: String, isCtrlOrCmd: Boolean, isShift: Boolean): Boolean {
        if (isCtrlOrCmd) {
            when (key.lowercase()) {
                "z" -> {
                    if (isShift) redo() else undo()
                    return true
                }
                "y" -> {
                    redo()
                    return true
                }
                "s" -> {
                    // Guardado rápido
                    return true
                }
            }
        } else {
            when (key.lowercase()) {
                "s", "v" -> {
                    setTool(Tool.SELECT)
                    return true
                }
                "w" -> {
                    setTool(Tool.WIRE)
                    return true
                }
                "h", " " -> {
                    setTool(Tool.PAN)
                    return true
                }
                "b" -> {
                    setTool(Tool.BOX)
                    return true
                }
                "r" -> {
                    rotateSelected()
                    return true
                }
                "g" -> {
                    toggleGrid()
                    return true
                }
                "m" -> {
                    toggleSnap()
                    return true
                }
                "delete", "backspace" -> {
                    deleteSelected()
                    return true
                }
                "t" -> {
                    setTool(Tool.TEXT)
                    return true
                }
            }
        }
        return false
    }

    // ==========================================
    // EXPORTACIÓN, IMPORTACIÓN Y DOCUMENTO
    // ==========================================

    fun getProjectDocument(): ProjectDocument {
        val s = _state.value
        return ProjectDocument(
            version = 1,
            metadata = ProjectMetadata(
                title = s.projectTitle,
                author = "Électricien Certifié",
                date = "2026-10-05",
                standardVersion = "NF C 15-100 / A5"
            ),
            panX = s.offset.x,
            panY = s.offset.y,
            scale = s.scale,
            gridSize = s.gridSize,
            elements = s.elements
        )
    }

    fun exportProjectJson(): String {
        return ProjectSerializer.toJson(getProjectDocument())
    }

    fun exportProjectSvg(): String {
        return SvgExporter.toSvg(getProjectDocument(), isWhiteBackground = _state.value.isWhiteBackground)
    }

    fun loadProjectJson(json: String): Boolean {
        val doc = ProjectSerializer.fromJson(json) ?: return false
        saveSnapshot()
        _state.update {
            it.copy(
                projectTitle = doc.metadata.title,
                offset = Offset(doc.panX, doc.panY),
                scale = doc.scale,
                gridSize = doc.gridSize
            )
        }
        recalculateGraph(doc.elements)
        showNotification("Projet chargé avec succès (${doc.elements.size} éléments)")
        return true
    }

    fun clearProject() {
        saveSnapshot()
        _state.update {
            it.copy(
                projectTitle = "Nouvelle installation",
                offset = Offset.Zero,
                scale = 1.0f
            )
        }
        recalculateGraph(emptyList())
        showNotification("Nouveau projet initialisé")
    }

    // ==========================================
    // INTERACCIÓN CON EL CANVAS (Eventos Pointer)
    // ==========================================

    fun onPointerDown(screenPoint: Offset) {
        val currentState = _state.value
        val rawWorld = SnapUtils.screenToWorld(screenPoint, currentState.offset, currentState.scale)
        val symbols = currentState.elements.filterIsInstance<SymbolElement>()

        val nearestTerminal = CircuitGraph.findNearestTerminal(rawWorld, symbols, maxDistance = 14f)
        val worldPoint = when {
            nearestTerminal != null -> nearestTerminal.second.second
            currentState.snapToGrid -> SnapUtils.snapToGrid(rawWorld, currentState.gridSize)
            else -> rawWorld
        }

        dragStartWorldPoint = worldPoint
        startTerminalAnchor = nearestTerminal?.let {
            TerminalAnchor(elementId = it.first.id, terminalId = it.second.first.id)
        }

        when (currentState.activeTool) {
            Tool.SELECT -> {
                val clickedElement = currentState.elements.lastOrNull { it.hits(rawWorld) }
                if (clickedElement != null) {
                    val wasSelected = clickedElement.isSelected
                    val updated = currentState.elements.map { el ->
                        if (el.id == clickedElement.id) el.copyWithSelected(!wasSelected)
                        else el.copyWithSelected(false)
                    }
                    _state.update { it.copy(elements = updated) }
                    isDraggingElements = true
                } else {
                    val deselected = currentState.elements.map { it.copyWithSelected(false) }
                    _state.update { it.copy(elements = deselected) }
                    isDraggingElements = false
                }
            }
            Tool.PAN -> {
                // Manejado en drag
            }
            Tool.WIRE -> {
                _state.update {
                    it.copy(
                        previewElement = WireElement(
                            id = nextId("wire_preview"),
                            start = worldPoint,
                            end = worldPoint,
                            color = it.activeWireColor,
                            startAnchor = startTerminalAnchor
                        ),
                        hoveredTerminalPos = nearestTerminal?.second?.second
                    )
                }
            }
            Tool.BOX -> {
                _state.update {
                    it.copy(
                        previewElement = BoxElement(
                            id = nextId("box_preview"),
                            topLeft = worldPoint,
                            bottomRight = worldPoint
                        )
                    )
                }
            }
            Tool.PLACE_SYMBOL -> {
                val symbolType = currentState.selectedSymbolTypeForPlacement
                if (symbolType != null) {
                    saveSnapshot()
                    val newSymbol = SymbolElement(
                        id = nextId("sym_${symbolType.name.lowercase()}"),
                        type = symbolType,
                        position = worldPoint
                    )
                    val newElements = currentState.elements + newSymbol
                    recalculateGraph(newElements)
                }
            }
            Tool.TEXT -> {
                saveSnapshot()
                val newText = TextElement(
                    id = nextId("txt"),
                    text = "Texte",
                    position = worldPoint,
                    isSelected = true
                )
                val deselected = currentState.elements.map { it.copyWithSelected(false) }
                val newElements = deselected + newText
                recalculateGraph(newElements)
                _state.update { it.copy(activeTool = Tool.SELECT) }
            }
        }
    }

    fun onPointerDrag(screenPoint: Offset, delta: Offset) {
        val currentState = _state.value
        val startPoint = dragStartWorldPoint
        val symbols = currentState.elements.filterIsInstance<SymbolElement>()

        when (currentState.activeTool) {
            Tool.PAN -> {
                pan(delta)
            }
            Tool.SELECT -> {
                if (isDraggingElements && startPoint != null) {
                    val rawWorld = SnapUtils.screenToWorld(screenPoint, currentState.offset, currentState.scale)
                    val worldPoint = if (currentState.snapToGrid) {
                        SnapUtils.snapToGrid(rawWorld, currentState.gridSize)
                    } else {
                        rawWorld
                    }
                    val diff = worldPoint - startPoint
                    if (diff != Offset.Zero) {
                        val moved = currentState.elements.map { el ->
                            if (el.isSelected) el.translate(diff) else el
                        }
                        _state.update { it.copy(elements = moved) }
                        dragStartWorldPoint = worldPoint
                    }
                } else {
                    pan(delta)
                }
            }
            Tool.WIRE -> {
                if (startPoint != null) {
                    val rawWorld = SnapUtils.screenToWorld(screenPoint, currentState.offset, currentState.scale)
                    val targetTerminal = CircuitGraph.findNearestTerminal(rawWorld, symbols, maxDistance = 14f)
                    val currentPoint = when {
                        targetTerminal != null -> targetTerminal.second.second
                        currentState.snapToGrid -> SnapUtils.snapToGrid(rawWorld, currentState.gridSize)
                        else -> rawWorld
                    }

                    _state.update {
                        it.copy(
                            previewElement = WireElement(
                                id = "preview",
                                start = startPoint,
                                end = currentPoint,
                                color = it.activeWireColor,
                                startAnchor = startTerminalAnchor,
                                endAnchor = targetTerminal?.let { t ->
                                    TerminalAnchor(elementId = t.first.id, terminalId = t.second.first.id)
                                }
                            ),
                            hoveredTerminalPos = targetTerminal?.second?.second
                        )
                    }
                }
            }
            Tool.BOX -> {
                if (startPoint != null) {
                    val rawWorld = SnapUtils.screenToWorld(screenPoint, currentState.offset, currentState.scale)
                    val currentPoint = if (currentState.snapToGrid) {
                        SnapUtils.snapToGrid(rawWorld, currentState.gridSize)
                    } else {
                        rawWorld
                    }

                    _state.update {
                        it.copy(
                            previewElement = BoxElement(
                                id = "preview",
                                topLeft = startPoint,
                                bottomRight = currentPoint
                            )
                        )
                    }
                }
            }
            Tool.PLACE_SYMBOL -> {
                val rawWorld = SnapUtils.screenToWorld(screenPoint, currentState.offset, currentState.scale)
                val currentPoint = if (currentState.snapToGrid) {
                    SnapUtils.snapToGrid(rawWorld, currentState.gridSize)
                } else {
                    rawWorld
                }
                currentState.selectedSymbolTypeForPlacement?.let { type ->
                    _state.update {
                        it.copy(
                            previewElement = SymbolElement(
                                id = "preview_sym",
                                type = type,
                                position = currentPoint
                            )
                        )
                    }
                }
            }
            Tool.TEXT -> {
                // El texto se coloca en onPointerDown y luego se edita/arrastra en SELECT
            }
        }
    }

    fun onPointerUp() {
        val currentState = _state.value
        val preview = currentState.previewElement

        if (preview != null && currentState.activeTool != Tool.PLACE_SYMBOL) {
            when (preview) {
                is WireElement -> {
                    if ((preview.endPoint - preview.startPoint).getDistance() > 3f) {
                        saveSnapshot()
                        val newWire = preview.copy(id = nextId("wire"))
                        val newElements = currentState.elements + newWire
                        recalculateGraph(newElements)
                    } else {
                        _state.update { it.copy(previewElement = null, hoveredTerminalPos = null) }
                    }
                }
                is BoxElement -> {
                    val b = preview.bounds()
                    if (b.width > 4f && b.height > 4f) {
                        saveSnapshot()
                        val newBox = preview.copy(id = nextId("box"))
                        val newElements = currentState.elements + newBox
                        recalculateGraph(newElements)
                    } else {
                        _state.update { it.copy(previewElement = null) }
                    }
                }
                else -> Unit
            }
        } else if (isDraggingElements) {
            saveSnapshot()
            recalculateGraph(currentState.elements)
        }

        isDraggingElements = false
        dragStartWorldPoint = null
        startTerminalAnchor = null
        _state.update { it.copy(hoveredTerminalPos = null) }
    }

    // ==========================================
    // RECALCULO DE GRAFO ELÉCTRICO Y AUDITORÍA NF C 15-100
    // ==========================================

    private fun recalculateGraph(elements: List<GraphicElement>) {
        val wires = elements.filterIsInstance<WireElement>()
        val symbols = elements.filterIsInstance<SymbolElement>()
        val junctions = CircuitGraph.findJunctionPoints(wires, symbols)
        val nets = CircuitGraph.buildNets(elements)
        val report = NFC15100Validator.validate(elements)

        _state.update {
            it.copy(
                elements = elements,
                previewElement = null,
                hoveredTerminalPos = null,
                junctionPoints = junctions,
                netsCount = nets.size,
                complianceReport = report
            )
        }
    }

    // ==========================================
    // HISTORIAL UNDO / REDO
    // ==========================================

    private fun saveSnapshot() {
        undoStack.add(_state.value.elements)
        redoStack.clear()
        updateHistoryFlags()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeLast()
            redoStack.add(_state.value.elements)
            recalculateGraph(previous)
            updateHistoryFlags()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.add(_state.value.elements)
            recalculateGraph(next)
            updateHistoryFlags()
        }
    }

    fun deleteSelected() {
        val currentElements = _state.value.elements
        val anySelected = currentElements.any { it.isSelected }
        if (anySelected) {
            saveSnapshot()
            val filtered = currentElements.filterNot { it.isSelected }
            recalculateGraph(filtered)
        }
    }

    private fun updateHistoryFlags() {
        _state.update {
            it.copy(
                canUndo = undoStack.isNotEmpty(),
                canRedo = redoStack.isNotEmpty()
            )
        }
    }
}
