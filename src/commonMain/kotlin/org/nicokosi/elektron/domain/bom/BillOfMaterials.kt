package org.nicokosi.elektron.domain.bom

import org.nicokosi.elektron.domain.model.BoxElement
import org.nicokosi.elektron.domain.model.GraphicElement
import org.nicokosi.elektron.domain.model.SymbolElement
import org.nicokosi.elektron.domain.model.WireElement
import kotlin.math.roundToInt

/**
 * Línea de artículo de la lista de materiales (BOM).
 */
data class BomItem(
    val reference: String,
    val designation: String,
    val category: String,
    val quantity: Int,
    val unit: String = "U"
)

/**
 * Resumen consolidado de materiales y conductores del proyecto.
 */
data class BillOfMaterialsReport(
    val items: List<BomItem>,
    val totalEstimatedWireMeters: Float
) {
    fun toCsv(): String {
        val sb = StringBuilder()
        sb.append("Référence;Désignation;Catégorie;Quantité;Unité\n")
        for (item in items) {
            sb.append("${item.reference};${item.designation};${item.category};${item.quantity};${item.unit}\n")
        }
        sb.append("CABLE-METRES;Conducteurs cuivre estimés;Câblage;${(totalEstimatedWireMeters * 10).roundToInt() / 10f};mètres\n")
        return sb.toString()
    }
}

object BillOfMaterialsCalculator {

    // Relación de escala estimada: 20px (1 casilla de grilla) ≈ 0.5 metros en plano unifilar/arquitectónico
    private const val PIXELS_PER_METER = 40.0f

    fun generateReport(elements: List<GraphicElement>): BillOfMaterialsReport {
        val symbols = elements.filterIsInstance<SymbolElement>()
        val wires = elements.filterIsInstance<WireElement>()
        val boxes = elements.filterIsInstance<BoxElement>()

        val items = mutableListOf<BomItem>()

        // 1. Agrupar símbolos por tipo y calibre/designación
        val groupedSymbols = symbols.groupBy { "${it.type.name}__${it.designation}" }
        for ((_, symList) in groupedSymbols) {
            val first = symList.first()
            items.add(
                BomItem(
                    reference = "ELEC-${first.type.name.take(4)}-${first.designation.filter { it.isLetterOrDigit() }.take(6)}",
                    designation = "${first.label} (${first.designation})",
                    category = first.type.category.displayName,
                    quantity = symList.size
                )
            )
        }

        // 2. Cajas de empalme / cuadros
        if (boxes.isNotEmpty()) {
            items.add(
                BomItem(
                    reference = "BOITIER-ENC",
                    designation = "Boîte de dérivation / Enveloppe de tableau",
                    category = "Enveloppes & Boîtiers",
                    quantity = boxes.size
                )
            )
        }

        // 3. Estimación de longitud de cables
        var totalPixels = 0f
        for (w in wires) {
            for (i in 0 until w.points.size - 1) {
                totalPixels += (w.points[i + 1] - w.points[i]).getDistance()
            }
        }
        val estimatedMeters = totalPixels / PIXELS_PER_METER

        return BillOfMaterialsReport(
            items = items.sortedBy { it.category },
            totalEstimatedWireMeters = estimatedMeters
        )
    }
}
