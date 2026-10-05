package org.nicokosi.elektron.ui.toolbar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nicokosi.elektron.domain.model.SymbolElement
import org.nicokosi.elektron.platform.LocalPlatformStorage
import org.nicokosi.elektron.viewmodel.CanvasViewModel
import org.nicokosi.elektron.viewmodel.Tool

/**
 * Barra de herramientas principal con diseño profesional, controles de proyecto y selector de color de cable.
 */
@Composable
fun Toolbar(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val storage = LocalPlatformStorage.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(Color(0xFF13141F))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Logotipo
            Text(
                text = "⚡ ELEKTRON",
                color = Color(0xFF64B5F6),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 4.dp)
            )

            // Alternar catálogo
            ToolButton(
                label = if (state.showSidebar) "◀ Biblio" else "▶ Biblio",
                isActive = state.showSidebar,
                highlightColor = Color(0xFF64B5F6),
                onClick = { viewModel.toggleSidebar() }
            )

            ToolbarSeparator()

            // Archivo / Persistencia
            ToolButton(
                label = "💾 Sauver",
                isActive = false,
                highlightColor = Color(0xFF81C784),
                onClick = {
                    val json = viewModel.exportProjectJson()
                    storage?.saveLocal(json)
                    viewModel.showNotification("Projet sauvegardé dans le navigateur !")
                }
            )
            ToolButton(
                label = "📂 Ouvrir",
                isActive = false,
                onClick = {
                    storage?.openFile(".elektron") { content ->
                        viewModel.loadProjectJson(content)
                    }
                }
            )
            ToolButton(
                label = "⬇ .elektron",
                isActive = false,
                onClick = {
                    val json = viewModel.exportProjectJson()
                    storage?.download("schema_electrique.elektron", json, "application/json")
                    viewModel.showNotification("Fichier .elektron téléchargé !")
                }
            )
            ToolButton(
                label = "🖼 .svg",
                isActive = false,
                highlightColor = Color(0xFFFFB74D),
                onClick = {
                    val svg = viewModel.exportProjectSvg()
                    storage?.download("plan_electrique_nfc15100.svg", svg, "image/svg+xml")
                    viewModel.showNotification("Plan vectoriel SVG exporté avec succès !")
                }
            )
            ToolButton(
                label = "📄 Nouveau",
                isActive = false,
                onClick = { viewModel.clearProject() }
            )

            ToolbarSeparator()

            // Herramientas de Interacción
            ToolButton(
                label = "↖ Sélect",
                isActive = state.activeTool == Tool.SELECT,
                onClick = { viewModel.setTool(Tool.SELECT) }
            )
            ToolButton(
                label = "✋ Pan",
                isActive = state.activeTool == Tool.PAN,
                onClick = { viewModel.setTool(Tool.PAN) }
            )
            ToolButton(
                label = "─ Câble",
                isActive = state.activeTool == Tool.WIRE,
                highlightColor = state.activeWireColor,
                onClick = { viewModel.setTool(Tool.WIRE) }
            )
            ToolButton(
                label = "▢ Boîte (B.D/B.R)",
                isActive = state.activeTool == Tool.BOX,
                onClick = { viewModel.setTool(Tool.BOX) }
            )
            ToolButton(
                label = "T Texte",
                isActive = state.activeTool == Tool.TEXT,
                highlightColor = Color(0xFFFFD54F),
                onClick = { viewModel.setTool(Tool.TEXT) }
            )

            // Selector rápido de color de cable activo
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A1D2B))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                WireColorDot(
                    color = Color(0xFFE53935),
                    tooltip = "Phase (Ph)",
                    isSelected = state.activeWireColor == Color(0xFFE53935),
                    onClick = {
                        viewModel.setActiveWireColor(Color(0xFFE53935))
                        viewModel.setTool(Tool.WIRE)
                    }
                )
                WireColorDot(
                    color = Color(0xFF1E88E5),
                    tooltip = "Neutre (N)",
                    isSelected = state.activeWireColor == Color(0xFF1E88E5),
                    onClick = {
                        viewModel.setActiveWireColor(Color(0xFF1E88E5))
                        viewModel.setTool(Tool.WIRE)
                    }
                )
                WireColorDot(
                    color = Color(0xFF43A047),
                    tooltip = "Terre (PE)",
                    isSelected = state.activeWireColor == Color(0xFF43A047),
                    onClick = {
                        viewModel.setActiveWireColor(Color(0xFF43A047))
                        viewModel.setTool(Tool.WIRE)
                    }
                )
                WireColorDot(
                    color = Color(0xFF8E24AA),
                    tooltip = "Retour Lampe / Navette",
                    isSelected = state.activeWireColor == Color(0xFF8E24AA),
                    onClick = {
                        viewModel.setActiveWireColor(Color(0xFF8E24AA))
                        viewModel.setTool(Tool.WIRE)
                    }
                )
                WireColorDot(
                    color = Color(0xFF64B5F6),
                    tooltip = "Unifilaire",
                    isSelected = state.activeWireColor == Color(0xFF64B5F6),
                    onClick = {
                        viewModel.setActiveWireColor(Color(0xFF64B5F6))
                        viewModel.setTool(Tool.WIRE)
                    }
                )
            }

            // Rotar componente seleccionado
            val selectedSymbol = state.elements.firstOrNull { it.isSelected && it is SymbolElement } as? SymbolElement
            if (selectedSymbol != null) {
                ToolButton(
                    label = "↻ (${selectedSymbol.rotationDegrees}°)",
                    isActive = false,
                    highlightColor = Color(0xFF29B6F6),
                    onClick = { viewModel.rotateSelected() }
                )
            }

            ToolbarSeparator()

            // Thème de fond (Blanc / Sombre)
            ToolButton(
                label = if (state.isWhiteBackground) "☀️ Blanc" else "🌙 Sombre",
                isActive = state.isWhiteBackground,
                highlightColor = Color(0xFFFFD54F),
                onClick = { viewModel.toggleWhiteBackground() }
            )

            // Rejilla e Imán
            ToolButton(
                label = if (state.showGrid) "▦ Grille" else "⬚ Grille",
                isActive = state.showGrid,
                onClick = { viewModel.toggleGrid() }
            )
            ToolButton(
                label = if (state.snapToGrid) "🧲 Aimant" else "⚪ Libre",
                isActive = state.snapToGrid,
                onClick = { viewModel.toggleSnap() }
            )

            // Undo / Redo
            ToolButton(
                label = "↶",
                isActive = false,
                enabled = state.canUndo,
                onClick = { viewModel.undo() }
            )
            ToolButton(
                label = "↷",
                isActive = false,
                enabled = state.canRedo,
                onClick = { viewModel.redo() }
            )

            // Borrado rápido
            if (state.elements.any { it.isSelected }) {
                ToolButton(
                    label = "🗑",
                    isActive = false,
                    highlightColor = Color(0xFFE57373),
                    onClick = { viewModel.deleteSelected() }
                )
            }

            ToolbarSeparator()

            // Presupuesto / BOM y Atajos
            ToolButton(
                label = "📋 Devis",
                isActive = state.showBomModal,
                highlightColor = Color(0xFF81C784),
                onClick = { viewModel.toggleBomModal(true) }
            )
            ToolButton(
                label = "⌨ Raccourcis",
                isActive = state.showShortcutsModal,
                onClick = { viewModel.toggleShortcutsModal(true) }
            )
            ToolButton(
                label = if (state.showProperties) "⚙ ▶" else "⚙ ◀",
                isActive = state.showProperties,
                onClick = { viewModel.toggleProperties() }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Indicadores
            if (state.netsCount > 0) {
                Text(
                    text = "⚡ ${state.netsCount} circ.",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Text(
                text = "${(state.scale * 100).toInt()}%",
                color = Color(0xFFE0E0E0),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Notificación de estado temporal
        state.statusNotification?.let { msg ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E3A5F))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ℹ $msg",
                    color = Color(0xFFECEFF1),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = Color(0xFF90CAF9),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable { viewModel.clearNotification() }
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun WireColorDot(
    color: Color,
    tooltip: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val border = if (isSelected) Color(0xFFECEFF1) else Color.Transparent
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .border(1.5.dp, border, RoundedCornerShape(3.dp))
            .clickable(onClick = onClick)
    )
}

@Composable
private fun ToolButton(
    label: String,
    isActive: Boolean,
    enabled: Boolean = true,
    highlightColor: Color? = null,
    onClick: () -> Unit
) {
    val activeBorder = highlightColor ?: Color(0xFF64B5F6)
    val activeBg = highlightColor?.copy(alpha = 0.2f) ?: Color(0xFF1E3A5F)
    val normalText = highlightColor ?: if (enabled) Color(0xFFD0D0D0) else Color(0xFF555555)

    val bgColor = when {
        !enabled -> Color.Transparent
        isActive -> activeBg
        else -> Color.Transparent
    }
    val borderColor = when {
        !enabled -> Color.Transparent
        isActive -> activeBorder
        else -> Color(0xFF262A3E)
    }
    val textColor = when {
        !enabled -> Color(0xFF555555)
        isActive -> activeBorder
        else -> normalText
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(4.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ToolbarSeparator() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(Color(0xFF2A2D45))
    )
}
