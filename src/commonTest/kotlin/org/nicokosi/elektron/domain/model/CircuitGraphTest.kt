package org.nicokosi.elektron.domain.model

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CircuitGraphTest {

    @Test
    fun testGenerateOrthogonalPathCollinear() {
        val start = Offset(100f, 100f)
        val end = Offset(200f, 100f)
        val path = WireElement.generateOrthogonalPath(start, end)
        assertEquals(2, path.size)
        assertEquals(start, path[0])
        assertEquals(end, path[1])
    }

    @Test
    fun testGenerateOrthogonalPathDiagonal() {
        val start = Offset(50f, 50f)
        val end = Offset(150f, 200f)
        val path = WireElement.generateOrthogonalPath(start, end)
        assertEquals(3, path.size)
        assertEquals(start, path[0])
        assertEquals(Offset(150f, 50f), path[1])
        assertEquals(end, path[2])
    }

    @Test
    fun testFindJunctionPointsConvergingWires() {
        val w1 = WireElement("w1", listOf(Offset(0f, 0f), Offset(100f, 0f)))
        val w2 = WireElement("w2", listOf(Offset(100f, 0f), Offset(100f, 100f)))
        val w3 = WireElement("w3", listOf(Offset(100f, 0f), Offset(200f, 0f)))

        val junctions = CircuitGraph.findJunctionPoints(listOf(w1, w2, w3), emptyList())
        assertEquals(1, junctions.size)
        assertEquals(100f, junctions[0].x)
        assertEquals(0f, junctions[0].y)
    }

    @Test
    fun testFindNearestTerminalSnapsWithinTolerance() {
        val sym = SymbolElement(
            id = "sym-1",
            type = SymbolType.CIRCUIT_BREAKER,
            position = Offset(100f, 100f)
        )
        val terminals = sym.getAbsoluteTerminals()
        assertTrue(terminals.isNotEmpty(), "El disyuntor debe tener bornas")

        val targetTerminal = terminals[0]
        val nearPoint = targetTerminal.second + Offset(3f, 3f)

        val result = CircuitGraph.findNearestTerminal(nearPoint, listOf(sym), maxDistance = 14f)
        assertNotNull(result)
        assertEquals("sym-1", result.first.id)
        assertEquals(targetTerminal.first.id, result.second.first.id)
    }

    @Test
    fun testBuildNetsGroupsConnectedWires() {
        val w1 = WireElement("w1", listOf(Offset(0f, 0f), Offset(50f, 0f)))
        val w2 = WireElement("w2", listOf(Offset(50f, 0f), Offset(100f, 0f)))
        val wIsolated = WireElement("w3", listOf(Offset(500f, 500f), Offset(600f, 500f)))

        val nets = CircuitGraph.buildNets(listOf(w1, w2, wIsolated))
        assertEquals(2, nets.size, "Debe identificar dos redes separadas")
        assertTrue(nets.any { it.wireIds.containsAll(listOf("w1", "w2")) })
        assertTrue(nets.any { it.wireIds.contains("w3") && it.wireIds.size == 1 })
    }
}
