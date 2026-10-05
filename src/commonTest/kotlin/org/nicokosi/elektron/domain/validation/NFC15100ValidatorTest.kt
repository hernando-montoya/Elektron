package org.nicokosi.elektron.domain.validation

import androidx.compose.ui.geometry.Offset
import org.nicokosi.elektron.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NFC15100ValidatorTest {

    @Test
    fun testEmptyProjectIsValid() {
        val report = NFC15100Validator.validate(emptyList())
        assertEquals(0, report.issues.size)
        assertEquals(0, report.totalCircuits)
        assertEquals(0, report.compliantCircuits)
    }

    @Test
    fun testMissingDifferentialReportsError() {
        val breaker = SymbolElement(
            id = "cb-1",
            type = SymbolType.CIRCUIT_BREAKER,
            position = Offset(100f, 100f)
        )
        val socket = SymbolElement(
            id = "so-1",
            type = SymbolType.POWER_OUTLET,
            position = Offset(200f, 100f)
        )
        val wire = WireElement(
            id = "w-1",
            points = listOf(breaker.position, socket.position),
            startAnchor = TerminalAnchor(breaker.id, "T1"),
            endAnchor = TerminalAnchor(socket.id, "L")
        )

        val report = NFC15100Validator.validate(listOf(breaker, socket, wire))
        assertTrue(
            report.issues.any { it.severity == IssueSeverity.ERROR && it.ruleCode == "NF-DDR-30MA" },
            "Debe reportar falta de diferencial de 30mA"
        )
    }

    @Test
    fun testDifferentialPresentPassesCheck() {
        val diff = SymbolElement(
            id = "diff-1",
            type = SymbolType.DIFFERENTIAL,
            position = Offset(50f, 100f)
        )
        val breaker = SymbolElement(
            id = "cb-1",
            type = SymbolType.CIRCUIT_BREAKER,
            position = Offset(150f, 100f)
        )
        val report = NFC15100Validator.validate(listOf(diff, breaker))
        assertTrue(
            report.issues.none { it.ruleCode == "NF-DDR-30MA" },
            "No debe reportar error NF-DDR-30MA si hay diferencial 30mA"
        )
    }

    @Test
    fun testCircuitWithMoreThan8SocketsTriggersWarning() {
        val diff = SymbolElement(id = "diff-1", type = SymbolType.DIFFERENTIAL, position = Offset.Zero)
        val breaker = SymbolElement(id = "cb-1", type = SymbolType.CIRCUIT_BREAKER, position = Offset(100f, 0f))
        val elements = mutableListOf<GraphicElement>(diff, breaker)

        // Crear circuito conectando 9 tomas con anchors al disyuntor
        var prevAnchor = TerminalAnchor(breaker.id, "OUT")
        for (i in 1..9) {
            val socket = SymbolElement(id = "sock-$i", type = SymbolType.POWER_OUTLET, position = Offset(100f + i * 50f, 0f))
            val currentAnchor = TerminalAnchor(socket.id, "IN")
            val wire = WireElement(
                id = "w-$i",
                points = listOf(Offset(100f + (i - 1) * 50f, 0f), socket.position),
                startAnchor = prevAnchor,
                endAnchor = currentAnchor
            )
            elements.add(socket)
            elements.add(wire)
            prevAnchor = TerminalAnchor(socket.id, "OUT")
        }

        val report = NFC15100Validator.validate(elements)
        assertTrue(
            report.issues.any { it.ruleCode == "NF-OUTLET-SECTION-REQ" },
            "Debe advertir sobre circuito de tomas con más de 8 puntos (NF-OUTLET-SECTION-REQ)"
        )
    }

    @Test
    fun testCircuitWithMoreThan8LightsTriggersWarning() {
        val diff = SymbolElement(id = "diff-1", type = SymbolType.DIFFERENTIAL, position = Offset.Zero)
        val breaker = SymbolElement(id = "cb-1", type = SymbolType.CIRCUIT_BREAKER, position = Offset(100f, 0f))
        val elements = mutableListOf<GraphicElement>(diff, breaker)

        var prevAnchor = TerminalAnchor(breaker.id, "OUT")
        for (i in 1..9) {
            val light = SymbolElement(id = "light-$i", type = SymbolType.LIGHT_POINT, position = Offset(100f + i * 50f, 0f))
            val currentAnchor = TerminalAnchor(light.id, "IN")
            val wire = WireElement(
                id = "w-$i",
                points = listOf(Offset(100f + (i - 1) * 50f, 0f), light.position),
                startAnchor = prevAnchor,
                endAnchor = currentAnchor
            )
            elements.add(light)
            elements.add(wire)
            prevAnchor = TerminalAnchor(light.id, "OUT")
        }

        val report = NFC15100Validator.validate(elements)
        assertTrue(
            report.issues.any { it.ruleCode == "NF-LIGHT-LIMIT-MAX" },
            "Debe advertir sobre circuito de iluminación con más de 8 puntos (NF-LIGHT-LIMIT-MAX)"
        )
    }
}

