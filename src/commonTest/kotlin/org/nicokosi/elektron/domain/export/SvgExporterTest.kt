package org.nicokosi.elektron.domain.export

import androidx.compose.ui.geometry.Offset
import org.nicokosi.elektron.domain.model.*
import kotlin.test.Test
import kotlin.test.assertTrue

class SvgExporterTest {

    @Test
    fun testEmptyDocumentGeneratesPlaceholderSvg() {
        val doc = ProjectDocument()
        val svg = SvgExporter.toSvg(doc, isWhiteBackground = false)
        assertTrue(svg.contains("<svg"), "Debe contener etiqueta raíz svg")
        assertTrue(svg.contains("Schéma électrique vierge"), "Debe indicar esquema vacío")
    }

    @Test
    fun testExportDarkThemeSvg() {
        val sym = SymbolElement(
            id = "sym-1",
            type = SymbolType.CIRCUIT_BREAKER,
            position = Offset(100f, 100f)
        )
        val text = TextElement(
            id = "txt-1",
            text = "Eclairage Salon",
            position = Offset(150f, 150f)
        )
        val doc = ProjectDocument(elements = listOf(sym, text))

        val svg = SvgExporter.toSvg(doc, isWhiteBackground = false)
        assertTrue(svg.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.contains("#161824"), "Debe usar fondo oscuro en modo estándar")
        assertTrue(svg.contains("Eclairage Salon"))
    }

    @Test
    fun testExportWhiteThemeSvg() {
        val sym = SymbolElement(
            id = "sym-1",
            type = SymbolType.DIFFERENTIAL,
            position = Offset(100f, 100f)
        )
        val doc = ProjectDocument(elements = listOf(sym))

        val svg = SvgExporter.toSvg(doc, isWhiteBackground = true)
        assertTrue(svg.contains("#FFFFFF"), "Debe usar fondo blanco en modo impresión/claro")
        assertTrue(svg.contains("NF C 15-100"), "Debe incluir cartela normativa")
    }
}
