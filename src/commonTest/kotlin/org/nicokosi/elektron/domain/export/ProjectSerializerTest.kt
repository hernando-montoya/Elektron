package org.nicokosi.elektron.domain.export

import androidx.compose.ui.geometry.Offset
import org.nicokosi.elektron.domain.model.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProjectSerializerTest {

    @Test
    fun testJsonRoundTripPreservesAllElements() {
        val meta = ProjectMetadata(
            title = "Tableau Principal Villa",
            author = "Ingénieur Électricien",
            date = "2026-10-05",
            standardVersion = "NF C 15-100:2026"
        )

        val breaker = SymbolElement(
            id = "sym-q1",
            type = SymbolType.CIRCUIT_BREAKER,
            position = Offset(100f, 150f),
            rotationDegrees = 0,
            label = "Q1",
            designation = "Disjoncteur 16A"
        )

        val socket = SymbolElement(
            id = "sym-pc1",
            type = SymbolType.POWER_OUTLET,
            position = Offset(300f, 150f),
            rotationDegrees = 0,
            label = "PC1",
            designation = "Prise 16A 2P+T"
        )

        val wire = WireElement(
            id = "wire-1",
            points = listOf(Offset(100f, 150f), Offset(200f, 150f), Offset(300f, 150f))
        )

        val box = BoxElement(
            id = "box-1",
            topLeft = Offset(50f, 50f),
            bottomRight = Offset(400f, 300f)
        )

        val text = TextElement(
            id = "text-1",
            text = "Zone Cuisine",
            position = Offset(60f, 60f),
            fontSize = 14f,
            isBold = true
        )

        val originalDoc = ProjectDocument(
            metadata = meta,
            panX = 10f,
            panY = 20f,
            scale = 1.25f,
            gridSize = 25f,
            elements = listOf(breaker, socket, wire, box, text)
        )

        val json = ProjectSerializer.toJson(originalDoc)
        assertTrue(json.contains("\"title\": \"Tableau Principal Villa\""))
        assertTrue(json.contains("\"kind\": \"SYMBOL\""))
        assertTrue(json.contains("\"kind\": \"WIRE\""))
        assertTrue(json.contains("\"kind\": \"BOX\""))
        assertTrue(json.contains("\"kind\": \"TEXT\""))

        val parsedDoc = ProjectSerializer.fromJson(json)
        assertNotNull(parsedDoc, "El documento deserializado no debe ser null")
        assertEquals(originalDoc.metadata.title, parsedDoc.metadata.title)
        assertEquals(originalDoc.metadata.author, parsedDoc.metadata.author)
        assertEquals(5, parsedDoc.elements.size)

        val parsedBreaker = parsedDoc.elements.filterIsInstance<SymbolElement>().firstOrNull { it.id == "sym-q1" }
        assertNotNull(parsedBreaker)
        assertEquals("Q1", parsedBreaker.label)
        assertEquals(SymbolType.CIRCUIT_BREAKER, parsedBreaker.type)

        val parsedWire = parsedDoc.elements.filterIsInstance<WireElement>().firstOrNull { it.id == "wire-1" }
        assertNotNull(parsedWire)
        assertEquals(3, parsedWire.points.size)

        val parsedBox = parsedDoc.elements.filterIsInstance<BoxElement>().firstOrNull { it.id == "box-1" }
        assertNotNull(parsedBox)
        assertEquals(Offset(50f, 50f), parsedBox.topLeft)

        val parsedText = parsedDoc.elements.filterIsInstance<TextElement>().firstOrNull { it.id == "text-1" }
        assertNotNull(parsedText)
        assertEquals("Zone Cuisine", parsedText.text)
        assertTrue(parsedText.isBold)
    }
}
