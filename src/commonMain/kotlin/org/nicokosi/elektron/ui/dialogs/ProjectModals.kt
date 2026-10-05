package org.nicokosi.elektron.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nicokosi.elektron.domain.bom.BillOfMaterialsCalculator
import org.nicokosi.elektron.platform.LocalPlatformStorage
import org.nicokosi.elektron.viewmodel.CanvasViewModel
import kotlin.math.roundToInt

@Composable
fun BomDialog(
    viewModel: CanvasViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val storage = LocalPlatformStorage.current
    val bom = BillOfMaterialsCalculator.generateReport(state.elements)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(620.dp)
                .height(480.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF131522))
                .border(1.dp, Color(0xFF2A2E44), RoundedCornerShape(8.dp))
                .clickable(enabled = false) {}
                .padding(18.dp)
        ) {
            // Título y botón cerrar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 NOMENCLATURE DES MATÉRIELS & DEVIS (BOM)",
                    color = Color(0xFF64B5F6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = Color(0xFF90A4AE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Cabecera de la tabla
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1E2F))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "RÉFÉRENCE", color = Color(0xFF90A4AE), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text(text = "DÉSIGNATION DU COMPOSANT", color = Color(0xFF90A4AE), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(2.5f))
                Text(text = "CATÉGORIE", color = Color(0xFF90A4AE), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.5f))
                Text(text = "QTÉ", color = Color(0xFF90A4AE), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp))
            }

            // Filas
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(bom.items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161928))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item.reference, color = Color(0xFF64B5F6), fontSize = 11.sp, modifier = Modifier.weight(1.5f))
                        Text(text = item.designation, color = Color(0xFFECEFF1), fontSize = 11.sp, modifier = Modifier.weight(2.5f))
                        Text(text = item.category, color = Color(0xFFB0BEC5), fontSize = 10.sp, modifier = Modifier.weight(1.5f))
                        Text(text = "${item.quantity} ${item.unit}", color = Color(0xFF81C784), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(50.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Resumen de cableado
            val meters = (bom.totalEstimatedWireMeters * 10).roundToInt() / 10f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF1A1E2F))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔌 Longueur totale de conducteurs estimée (filerie):",
                    color = Color(0xFFECEFF1),
                    fontSize = 11.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "$meters mètres",
                    color = Color(0xFF64B5F6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Botón de exportación a CSV
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF1E3A5F))
                        .border(1.dp, Color(0xFF64B5F6), RoundedCornerShape(4.dp))
                        .clickable {
                            val csv = bom.toCsv()
                            storage?.download("nomenclature_materiels.csv", csv, "text/csv")
                            viewModel.showNotification("Nomenclature CSV téléchargée !")
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(text = "📥 Télécharger CSV (Excel)", color = Color(0xFF64B5F6), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ShortcutsDialog(
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(460.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF131522))
                .border(1.dp, Color(0xFF2A2E44), RoundedCornerShape(8.dp))
                .clickable(enabled = false) {}
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⌨ RACCOURCIS CLAVIER",
                    color = Color(0xFF64B5F6),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "✕",
                    color = Color(0xFF90A4AE),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val shortcuts = listOf(
                "S ou V" to "Outil de sélection d'objets",
                "W" to "Outil de tracé de câble orthogonal (90°)",
                "H ou Espace" to "Outil de déplacement (Pan)",
                "B" to "Boîte de dérivation / Enveloppe",
                "T" to "Outil texte libre / Annotation",
                "R" to "Pivoter le composant sélectionné (+90°)",
                "Suppr / Backspace" to "Supprimer le composant sélectionné",
                "G" to "Afficher / Masquer la grille",
                "M" to "Activer / Désactiver l'aimant magnétique (Snap)",
                "Ctrl+Z" to "Annuler la dernière action (Undo)",
                "Ctrl+Y" to "Rétablir l'action (Redo)"
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((key, desc) in shortcuts) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF161928))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF1E2336))
                                .border(1.dp, Color(0xFF3B4260), RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = key, color = Color(0xFF64B5F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = desc, color = Color(0xFFCFD8DC), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
