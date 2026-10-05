package org.nicokosi.elektron.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nicokosi.elektron.domain.model.SymbolCategory
import org.nicokosi.elektron.domain.model.SymbolType
import org.nicokosi.elektron.viewmodel.CanvasViewModel

@Composable
fun ComponentLibrarySidebar(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    var selectedCategory by remember { mutableStateOf<SymbolCategory?>(SymbolCategory.SCHEMA_DEVELOPPE) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(270.dp)
            .background(Color(0xFF131522))
            .border(width = 1.dp, color = Color(0xFF222638))
            .padding(10.dp)
    ) {
        // Encabezado
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            Text(
                text = "BIBLIOTHÈQUE DE COMPOSANTS",
                color = Color(0xFF64B5F6),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Filtro por tipo de esquema / categoría
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CategoryChip(
                    label = "Développé",
                    isSelected = selectedCategory == SymbolCategory.SCHEMA_DEVELOPPE,
                    onClick = { selectedCategory = SymbolCategory.SCHEMA_DEVELOPPE }
                )
                CategoryChip(
                    label = "Multifilaire",
                    isSelected = selectedCategory == SymbolCategory.SCHEMA_MULTIFILAIRE,
                    onClick = { selectedCategory = SymbolCategory.SCHEMA_MULTIFILAIRE }
                )
                CategoryChip(
                    label = "Tous",
                    isSelected = selectedCategory == null,
                    onClick = { selectedCategory = null }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                CategoryChip(
                    label = "Tableau",
                    isSelected = selectedCategory == SymbolCategory.PROTECTION,
                    onClick = { selectedCategory = SymbolCategory.PROTECTION }
                )
                CategoryChip(
                    label = "Prises",
                    isSelected = selectedCategory == SymbolCategory.OUTLETS,
                    onClick = { selectedCategory = SymbolCategory.OUTLETS }
                )
                CategoryChip(
                    label = "Éclairage",
                    isSelected = selectedCategory == SymbolCategory.LIGHTING_SWITCHES,
                    onClick = { selectedCategory = SymbolCategory.LIGHTING_SWITCHES }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Lista de símbolos
        val filteredTypes = SymbolType.values().filter {
            selectedCategory == null || it.category == selectedCategory
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(filteredTypes) { type ->
                val isSelectedType = state.selectedSymbolTypeForPlacement == type

                SymbolCard(
                    symbolType = type,
                    isSelected = isSelectedType,
                    onClick = {
                        viewModel.selectSymbolForPlacement(type)
                    }
                )
            }
        }

        // Pie informativo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF1B2033))
                .padding(8.dp)
        ) {
            Column {
                Text(
                    text = "Norme NF C 15-100",
                    color = Color(0xFF81C784),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Schémas développés (linéaires) & multifilaires (câblage réel).",
                    color = Color(0xFFAAAAAA),
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF1E3A5F) else Color(0xFF1A1D2B)
    val textColor = if (isSelected) Color(0xFF64B5F6) else Color(0xFF888888)
    val border = if (isSelected) Color(0xFF64B5F6) else Color(0xFF282D42)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SymbolCard(
    symbolType: SymbolType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) Color(0xFF243655) else Color(0xFF181C2B)
    val borderColor = if (isSelected) Color(0xFF64B5F6) else Color(0xFF282D42)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icono característico
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF23273D)),
            contentAlignment = Alignment.Center
        ) {
            val glyph = when (symbolType) {
                SymbolType.BUS_LINE_PH -> "🔴"
                SymbolType.BUS_LINE_N -> "🔵"
                SymbolType.BUS_LINE_PE -> "🟢"
                SymbolType.POLE_DISJONCTEUR_PH -> "⚡"
                SymbolType.POLE_DISJONCTEUR_N -> "⏚"
                SymbolType.CONTACT_INTERRUPTEUR -> "╱"
                SymbolType.LAMPE_AVEC_TERRE -> "💡"
                SymbolType.DISJONCTEUR_MULTI -> "⚡⚡"
                SymbolType.DIFFERENTIEL_MULTI -> "🛡"
                SymbolType.INTERRUPTEUR_MURAL -> "🔲"
                SymbolType.LAMPE_DCL_MULTI -> "🔆"
                SymbolType.BORNE_WAGO -> "▪▪▪"
                SymbolType.BARRETTE_TERRE -> "⏚"
                SymbolType.CIRCUIT_BREAKER -> "⚡"
                SymbolType.DIFFERENTIAL -> "🛡"
                SymbolType.POWER_OUTLET -> "🔌"
                SymbolType.LIGHT_POINT -> "💡"
                SymbolType.SWITCH_SIMPLE -> "🔘"
            }
            Text(text = glyph, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = symbolType.defaultLabel,
                color = Color(0xFFECEFF1),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = symbolType.description,
                color = Color(0xFF90A4AE),
                fontSize = 9.sp,
                maxLines = 1
            )
            Text(
                text = symbolType.standardRating,
                color = Color(0xFF64B5F6),
                fontSize = 9.sp
            )
        }
    }
}
