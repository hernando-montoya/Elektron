package org.nicokosi.elektron.app

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import org.nicokosi.elektron.ui.canvas.ElektronCanvas
import org.nicokosi.elektron.ui.dialogs.BomDialog
import org.nicokosi.elektron.ui.dialogs.ShortcutsDialog
import org.nicokosi.elektron.ui.properties.PropertiesPanel
import org.nicokosi.elektron.ui.sidebar.ComponentLibrarySidebar
import org.nicokosi.elektron.ui.toolbar.Toolbar
import org.nicokosi.elektron.ui.validation.ValidationReportPanel
import org.nicokosi.elektron.viewmodel.CanvasViewModel

// Paleta oscura técnica para software CAD profesional
private val ElektronDarkColors = darkColorScheme(
    primary = Color(0xFF64B5F6),
    onPrimary = Color(0xFF131522),
    primaryContainer = Color(0xFF1E3A5F),
    secondary = Color(0xFF81C784),
    background = Color(0xFF131522),
    surface = Color(0xFF181C2B),
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
)

@Composable
fun App() {
    val viewModel = remember { CanvasViewModel() }
    val state by viewModel.state.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    MaterialTheme(colorScheme = ElektronDarkColors) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .focusRequester(focusRequester)
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            val key = when (keyEvent.key) {
                                Key.S -> "s"
                                Key.V -> "v"
                                Key.W -> "w"
                                Key.H -> "h"
                                Key.B -> "b"
                                Key.R -> "r"
                                Key.G -> "g"
                                Key.M -> "m"
                                Key.Z -> "z"
                                Key.Y -> "y"
                                Key.T -> "t"
                                Key.Delete -> "delete"
                                Key.Backspace -> "backspace"
                                Key.Spacebar -> " "
                                else -> ""
                            }
                            if (key.isNotEmpty()) {
                                viewModel.handleKeyDown(
                                    key = key,
                                    isCtrlOrCmd = keyEvent.isCtrlPressed || keyEvent.isMetaPressed,
                                    isShift = keyEvent.isShiftPressed
                                )
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
            ) {
                // 1. Barra superior
                Toolbar(viewModel = viewModel)

                // 2. Área de trabajo triple: Catálogo (Izq) + Lienzo (Centro) + Propiedades (Der)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (state.showSidebar) {
                        ComponentLibrarySidebar(
                            viewModel = viewModel
                        )
                    }

                    ElektronCanvas(
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )

                    if (state.showProperties) {
                        PropertiesPanel(
                            viewModel = viewModel
                        )
                    }
                }

                // 3. Panel inferior de auditoría normativa NF C 15-100
                ValidationReportPanel(
                    viewModel = viewModel
                )
            }

            // 4. Modales flotantes (BOM y Atajos de teclado)
            if (state.showBomModal) {
                BomDialog(
                    viewModel = viewModel,
                    onDismiss = { viewModel.toggleBomModal(false) }
                )
            }

            if (state.showShortcutsModal) {
                ShortcutsDialog(
                    onDismiss = { viewModel.toggleShortcutsModal(false) }
                )
            }
        }
    }
}
