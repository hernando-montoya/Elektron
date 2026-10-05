package org.nicokosi.elektron.ui.validation

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
import org.nicokosi.elektron.domain.validation.IssueSeverity
import org.nicokosi.elektron.domain.validation.ValidationIssue
import org.nicokosi.elektron.viewmodel.CanvasViewModel

@Composable
fun ValidationReportPanel(
    viewModel: CanvasViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val report = state.complianceReport
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF131522))
            .border(width = 1.dp, color = Color(0xFF222638))
    ) {
        // Barra de encabezado interactiva (siempre visible como barra de estado)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador de estado de conformidad general
            val statusColor = when {
                report.errorCount > 0 -> Color(0xFFEF5350) // Rojo
                report.warningCount > 0 -> Color(0xFFFFCA28) // Amarillo
                report.totalCircuits > 0 -> Color(0xFF66BB6A) // Verde
                else -> Color(0xFF9E9E9E)
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "AUDIT NORMATIF NF C 15-100",
                color = Color(0xFFE0E0E0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Badges de conteo
            if (report.errorCount > 0) {
                StatusBadge(
                    label = "${report.errorCount} Erreur(s)",
                    color = Color(0xFFEF5350)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (report.warningCount > 0) {
                StatusBadge(
                    label = "${report.warningCount} Alerte(s)",
                    color = Color(0xFFFFCA28)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (report.isFullyCompliant) {
                StatusBadge(
                    label = "✓ Conforme (${report.compliantCircuits} circuits)",
                    color = Color(0xFF66BB6A)
                )
            } else if (report.totalCircuits == 0) {
                Text(
                    text = "En attente de schéma...",
                    color = Color(0xFF757575),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Botón de expansión
            Text(
                text = if (isExpanded) "▼ Réduire" else "▲ Détails de conformité",
                color = Color(0xFF64B5F6),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Panel desplegable con la lista detallada de no conformidades
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color(0xFF0F111C))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (report.issues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (report.totalCircuits > 0) "✓ Tous les circuits respectent scrupuleusement la norme NF C 15-100."
                                   else "Dessinez des circuits pour lancer l'audit automatique en temps réel.",
                            color = if (report.totalCircuits > 0) Color(0xFF81C784) else Color(0xFF757575),
                            fontSize = 12.sp
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(report.issues) { issue ->
                            IssueRow(
                                issue = issue,
                                onClick = {
                                    viewModel.selectElementsById(issue.relatedElementIds)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IssueRow(
    issue: ValidationIssue,
    onClick: () -> Unit
) {
    val borderColor = when (issue.severity) {
        IssueSeverity.ERROR -> Color(0xFFEF5350)
        IssueSeverity.WARNING -> Color(0xFFFFCA28)
        IssueSeverity.INFO -> Color(0xFF42A5F5)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF161928))
            .border(1.dp, borderColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (issue.severity) {
            IssueSeverity.ERROR -> "⛔"
            IssueSeverity.WARNING -> "⚠️"
            IssueSeverity.INFO -> "ℹ️"
        }

        Text(text = icon, fontSize = 14.sp)
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = issue.title,
                    color = Color(0xFFECEFF1),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = issue.standardReference,
                    color = Color(0xFF64B5F6),
                    fontSize = 10.sp
                )
            }
            Text(
                text = issue.description,
                color = Color(0xFFB0BEC5),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }

        if (issue.relatedElementIds.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Localiser →",
                color = Color(0xFF64B5F6),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
