package org.nicokosi.elektron.domain.bom

import androidx.compose.ui.geometry.Offset
import org.nicokosi.elektron.domain.model.BoxElement
import org.nicokosi.elektron.domain.model.SymbolElement
import org.nicokosi.elektron.domain.model.SymbolType
import org.nicokosi.elektron.domain.model.WireElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BillOfMaterialsTest {

    @Test
    fun testBomEmptyProject() {
        val report = BillOfMaterialsCalculator.generateReport(emptyList())
        assertEquals(0, report.items.size)
        assertEquals(0f, report.totalEstimatedWireMeters)
    }

    @Test
    fun testBomGroupsSymbolsAndBoxes() {
        val b1 = SymbolElement("b1", SymbolType.CIRCUIT_BREAKER, Offset.Zero, designation = "16A")
        val b2 = SymbolElement("b2", SymbolType.CIRCUIT_BREAKER, Offset(10f, 0f), designation = "16A")
        val diff = SymbolElement("d1", SymbolType.DIFFERENTIAL, Offset(20f, 0f), designation = "40A 30mA")
        val box = BoxElement("box1", Offset(0f, 0f), Offset(100f, 100f))
        val wire = WireElement("w1", listOf(Offset(0f, 0f), Offset(80f, 0f))) // 80px = 2.0 meters (80 / 40)

        val report = BillOfMaterialsCalculator.generateReport(listOf(b1, b2, diff, box, wire))

        // 2 circuit breakers (grouped together) + 1 differential + 1 box = 3 items in list
        assertEquals(3, report.items.size)

        val breakerItem = report.items.firstOrNull { it.designation.contains("16A") }
        assertTrue(breakerItem != null)
        assertEquals(2, breakerItem.quantity)

        val boxItem = report.items.firstOrNull { it.reference == "BOITIER-ENC" }
        assertTrue(boxItem != null)
        assertEquals(1, boxItem.quantity)

        assertEquals(2.0f, report.totalEstimatedWireMeters)

        val csv = report.toCsv()
        assertTrue(csv.contains("Référence;Désignation;Catégorie;Quantité;Unité"))
        assertTrue(csv.contains("CABLE-METRES;Conducteurs cuivre estimés;Câblage;2.0;mètres"))
    }
}
