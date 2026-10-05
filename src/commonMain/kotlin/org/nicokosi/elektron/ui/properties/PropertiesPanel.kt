package org.nicokosi.elektron.ui.properties

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nicokosi.elektron.domain.model.BoxElement
import org.nicokosi.elektron.domain.model.SymbolElement
import org.nicokosi.elektron.domain.model.TextElement
import org.nicokosi.elektron.domain.model.WireElement
import org.nicokosi.elektron.viewmodel.CanvasViewModel
import kotlin.math.roundToInt

@Composable
fun PropertiesPanel(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val selectedElement = state.elements.firstOrNull { it.isSelected }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp)
            .background(Color(0xFF131522))
            .border(width = 1.dp, color = Color(0xFF222638))
            .padding(12.dp)
    ) {
        // Encabezado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Text(
                text = "PROPRIÉTÉS DU COMPOSANT",
                color = Color(0xFF64B5F6),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        if (selectedElement == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF181B2B))
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aucun objet sélectionné.\nCliquez sur un symbole ou un câble pour ajuster ses caractéristiques techniques.",
                    color = Color(0xFF757575),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedElement) {
                    is SymbolElement -> {
                        PropertyHeader(title = "Appareil: ${selectedElement.type.defaultLabel}")

                        // Campo de Etiqueta
                        PropertyLabel(text = "Repère / Identifiant")
                        EditableField(
                            value = selectedElement.label,
                            onValueChange = { newLabel ->
                                viewModel.updateSelectedSymbolProperties(label = newLabel)
                            }
                        )

                        // Calibre / Designación técnica
                        PropertyLabel(text = "Calibre / Caractéristique NF C 15-100")
                        val ratings = when (selectedElement.type.name) {
                            "CIRCUIT_BREAKER" -> listOf("10A / 3kA", "16A / 3kA", "20A / 3kA", "32A / 3kA")
                            "DIFFERENTIAL" -> listOf("40A 30mA Type AC", "40A 30mA Type A", "63A 30mA Type A")
                            "POWER_OUTLET" -> listOf("16A - 230V 2P+T", "20A Spécialisée", "32A Plaque cuisson")
                            "LIGHT_POINT" -> listOf("DCL 10A max", "Spot LED 230V", "Applique murale")
                            else -> listOf(selectedElement.designation)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (r in ratings) {
                                RatingOptionButton(
                                    label = r,
                                    isSelected = selectedElement.designation == r,
                                    onClick = {
                                        viewModel.updateSelectedSymbolProperties(designation = r)
                                    }
                                )
                            }
                        }

                        // Orientación
                        PropertyLabel(text = "Orientation")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ActionMiniButton(
                                label = "↻ +90° (${selectedElement.rotationDegrees}°)",
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.rotateSelected() }
                            )
                        }
                    }

                    is WireElement -> {
                        PropertyHeader(title = "Conducteur électrique")

                        // Longitud calculada
                        var lenPx = 0f
                        for (i in 0 until selectedElement.points.size - 1) {
                            lenPx += (selectedElement.points[i + 1] - selectedElement.points[i]).getDistance()
                        }
                        val lenM = (lenPx / 40.0f * 10).roundToInt() / 10f

                        PropertyLabel(text = "Longueur estimée: $lenM mètres")
                        PropertyLabel(text = "Type de conducteur (Couleur NF C 15-100)")

                        // Opciones de función de cable
                        ColorOptionButton(
                            label = "Fase (Rouge/Marron)",
                            color = Color(0xFFE53935),
                            isSelected = selectedElement.color == Color(0xFFE53935),
                            onClick = { viewModel.updateSelectedWireColor(Color(0xFFE53935)) }
                        )
                        ColorOptionButton(
                            label = "Neutre (Bleu)",
                            color = Color(0xFF1E88E5),
                            isSelected = selectedElement.color == Color(0xFF1E88E5),
                            onClick = { viewModel.updateSelectedWireColor(Color(0xFF1E88E5)) }
                        )
                        ColorOptionButton(
                            label = "Terre PE (Vert/Jaune)",
                            color = Color(0xFF43A047),
                            isSelected = selectedElement.color == Color(0xFF43A047),
                            onClick = { viewModel.updateSelectedWireColor(Color(0xFF43A047)) }
                        )
                        ColorOptionButton(
                            label = "Schéma unifilaire (Standard)",
                            color = Color(0xFF64B5F6),
                            isSelected = selectedElement.color == Color(0xFF64B5F6),
                            onClick = { viewModel.updateSelectedWireColor(Color(0xFF64B5F6)) }
                        )
                    }

                    is BoxElement -> {
                        PropertyHeader(title = "Boîtier / Enveloppe")
                        val b = selectedElement.bounds()
                        PropertyLabel(text = "Largeur: ${b.width.toInt()} px | Hauteur: ${b.height.toInt()} px")
                    }

                    is TextElement -> {
                        PropertyHeader(title = "Annotation / Texte libre")

                        PropertyLabel(text = "Texte affiché")
                        EditableField(
                            value = selectedElement.text,
                            onValueChange = { newText ->
                                viewModel.updateSelectedText(text = newText)
                            }
                        )

                        PropertyLabel(text = "Taille de police")
                        val sizes = listOf(
                            10f to "10px (Légende)",
                            13f to "13px (Standard)",
                            18f to "18px (Titre)",
                            24f to "24px (En-tête)"
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            for ((sz, lbl) in sizes) {
                                RatingOptionButton(
                                    label = lbl,
                                    isSelected = selectedElement.fontSize == sz,
                                    onClick = { viewModel.updateSelectedText(fontSize = sz) }
                                )
                            }
                        }

                        PropertyLabel(text = "Style du texte")
                        ActionMiniButton(
                            label = if (selectedElement.isBold) "✓ Gras (Bold)" else "Normal",
                            color = if (selectedElement.isBold) Color(0xFFFFB74D) else Color(0xFF64B5F6),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.updateSelectedText(isBold = !selectedElement.isBold) }
                        )

                        PropertyLabel(text = "Couleur du texte")
                        ColorOptionButton(
                            label = "Automatique (Contraste)",
                            color = if (state.isWhiteBackground) Color(0xFF0F172A) else Color(0xFFECEFF1),
                            isSelected = selectedElement.color == Color.Unspecified,
                            onClick = { viewModel.updateSelectedText(color = Color.Unspecified) }
                        )
                        ColorOptionButton(
                            label = "Phase (Rouge)",
                            color = Color(0xFFE53935),
                            isSelected = selectedElement.color == Color(0xFFE53935),
                            onClick = { viewModel.updateSelectedText(color = Color(0xFFE53935)) }
                        )
                        ColorOptionButton(
                            label = "Neutre (Bleu)",
                            color = Color(0xFF1E88E5),
                            isSelected = selectedElement.color == Color(0xFF1E88E5),
                            onClick = { viewModel.updateSelectedText(color = Color(0xFF1E88E5)) }
                        )
                        ColorOptionButton(
                            label = "Terre PE (Vert)",
                            color = Color(0xFF43A047),
                            isSelected = selectedElement.color == Color(0xFF43A047),
                            onClick = { viewModel.updateSelectedText(color = Color(0xFF43A047)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Botón de eliminar
                ActionMiniButton(
                    label = "🗑 Supprimer le composant",
                    color = Color(0xFFEF5350),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.deleteSelected() }
                )
            }
        }
    }
}

@Composable
private fun PropertyHeader(title: String) {
    Text(
        text = title,
        color = Color(0xFFECEFF1),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
private fun PropertyLabel(text: String) {
    Text(
        text = text,
        color = Color(0xFF90A4AE),
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )
}

@Composable
private fun EditableField(
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF64B5F6),
            unfocusedBorderColor = Color(0xFF262D42),
            focusedTextColor = Color(0xFFECEFF1),
            unfocusedTextColor = Color(0xFFECEFF1),
            focusedContainerColor = Color(0xFF161928),
            unfocusedContainerColor = Color(0xFF161928)
        ),
        modifier = Modifier.fillMaxWidth().height(44.dp)
    )
}

@Composable
private fun RatingOptionButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF161928)
    val border = if (isSelected) Color(0xFF64B5F6) else Color(0xFF262D42)
    val textColor = if (isSelected) Color(0xFF64B5F6) else Color(0xFFB0BEC5)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 11.sp)
    }
}

@Composable
private fun ColorOptionButton(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val border = if (isSelected) Color(0xFFECEFF1) else Color(0xFF262D42)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF161928))
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = Color(0xFFCFD8DC), fontSize = 11.sp)
    }
}

@Composable
private fun ActionMiniButton(
    label: String,
    color: Color = Color(0xFF64B5F6),
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
