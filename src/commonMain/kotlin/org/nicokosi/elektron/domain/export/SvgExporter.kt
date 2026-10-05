package org.nicokosi.elektron.domain.export

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.nicokosi.elektron.domain.model.*
import kotlin.math.max
import kotlin.math.min

/**
 * Generador de gráficos vectoriales SVG estándar para esquemas eléctricos NF C 15-100.
 */
object SvgExporter {

    fun toSvg(doc: ProjectDocument, isWhiteBackground: Boolean = false): String {
        val bgHex = if (isWhiteBackground) "#FFFFFF" else "#161824"
        val boxStrokeHex = if (isWhiteBackground) "#0288D1" else "#64B5F6"
        val boxFillHex = if (isWhiteBackground) "#E1F5FE" else "#1E3A5F"
        val symbolShapeStroke = if (isWhiteBackground) "#0F172A" else "#ECEFF1"
        val symbolShapeFill = if (isWhiteBackground) "#FFFFFF" else "#1E2333"
        val symbolLine = if (isWhiteBackground) "#0F172A" else "#ECEFF1"
        val junctionFill = if (isWhiteBackground) "#0F172A" else "#00E5FF"
        val labelTitleFill = if (isWhiteBackground) "#0F172A" else "#ECEFF1"
        val labelSubFill = if (isWhiteBackground) "#0288D1" else "#64B5F6"
        val titleBlockFill = if (isWhiteBackground) "#F8FAFC" else "#131522"
        val titleBlockStroke = if (isWhiteBackground) "#CBD5E1" else "#2A2D45"
        val titleBlockTitle = if (isWhiteBackground) "#0288D1" else "#64B5F6"
        val titleBlockText = if (isWhiteBackground) "#0F172A" else "#E0E0E0"
        val titleBlockSub = if (isWhiteBackground) "#64748B" else "#9E9E9E"

        if (doc.elements.isEmpty()) {
            return """<svg xmlns="http://www.w3.org/2000/svg" width="800" height="600" viewBox="0 0 800 600">
  <rect width="100%" height="100%" fill="$bgHex"/>
  <text x="50%" y="50%" fill="#888" font-family="sans-serif" font-size="16" text-anchor="middle">Schéma électrique vierge</text>
</svg>"""
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (el in doc.elements) {
            val b = el.bounds()
            minX = min(minX, b.left)
            minY = min(minY, b.top)
            maxX = max(maxX, b.right)
            maxY = max(maxY, b.bottom)
        }

        val padding = 60f
        val vbX = (minX - padding).toInt()
        val vbY = (minY - padding).toInt()
        val vbW = (maxX - minX + padding * 2).toInt()
        val vbH = (maxY - minY + padding * 2).toInt()

        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="$vbW" height="$vbH" viewBox="$vbX $vbY $vbW $vbH">
  <defs>
    <style>
      .wire { stroke-width: 2.5; stroke-linecap: round; stroke-linejoin: round; fill: none; }
      .box { stroke: $boxStrokeHex; stroke-width: 1.8; fill: $boxFillHex; fill-opacity: 0.15; rx: 6px; }
      .symbol-shape { stroke: $symbolShapeStroke; stroke-width: 1.8; fill: $symbolShapeFill; }
      .symbol-line { stroke: $symbolLine; stroke-width: 1.8; stroke-linecap: round; }
      .symbol-detail { stroke: $symbolLine; stroke-width: 1.2; fill: none; }
      .terminal { fill: #29B6F6; stroke: #0288D1; stroke-width: 1.0; }
      .junction { fill: $junctionFill; }
      .label-title { fill: $labelTitleFill; font-family: monospace, sans-serif; font-size: 11px; font-weight: bold; }
      .label-sub { fill: $labelSubFill; font-family: monospace, sans-serif; font-size: 9px; }
      .title-block { fill: $titleBlockFill; stroke: $titleBlockStroke; stroke-width: 1.5; }
    </style>
  </defs>
  <!-- Fond du schéma -->
  <rect x="$vbX" y="$vbY" width="$vbW" height="$vbH" fill="$bgHex"/>
""")

        // Cajas / Envolventes
        for (box in doc.elements.filterIsInstance<BoxElement>()) {
            val b = box.bounds()
            sb.append("  <rect class=\"box\" x=\"${b.left}\" y=\"${b.top}\" width=\"${b.width}\" height=\"${b.height}\"/>\n")
        }

        // Cables con sus colores específicos de conductor
        for (wire in doc.elements.filterIsInstance<WireElement>()) {
            val pts = wire.points.joinToString(" ") { "${it.x},${it.y}" }
            val hexColor = colorToHex(wire.color)
            sb.append("  <polyline class=\"wire\" points=\"$pts\" stroke=\"$hexColor\"/>\n")
            sb.append("  <circle cx=\"${wire.startPoint.x}\" cy=\"${wire.startPoint.y}\" r=\"3\" fill=\"$hexColor\"/>\n")
            sb.append("  <circle cx=\"${wire.endPoint.x}\" cy=\"${wire.endPoint.y}\" r=\"3\" fill=\"$hexColor\"/>\n")
        }

        // Nodos de derivación
        val wires = doc.elements.filterIsInstance<WireElement>()
        val symbols = doc.elements.filterIsInstance<SymbolElement>()
        val junctions = CircuitGraph.findJunctionPoints(wires, symbols)
        for (j in junctions) {
            sb.append("  <circle class=\"junction\" cx=\"${j.x}\" cy=\"${j.y}\" r=\"4\"/>\n")
        }

        // Símbolos
        for (sym in symbols) {
            sb.append(renderSymbolSvg(sym))
        }

        // Textos libres y anotaciones
        for (textEl in doc.elements.filterIsInstance<TextElement>()) {
            val hexColor = when {
                textEl.color != Color.Unspecified -> colorToHex(textEl.color)
                isWhiteBackground -> "#0F172A"
                else -> "#ECEFF1"
            }
            val fontWeight = if (textEl.isBold) "bold" else "normal"
            val textY = textEl.position.y + textEl.fontSize
            sb.append("  <text x=\"${textEl.position.x}\" y=\"$textY\" fill=\"$hexColor\" font-family=\"sans-serif\" font-size=\"${textEl.fontSize}\" font-weight=\"$fontWeight\">${escapeXml(textEl.text)}</text>\n")
        }

        // Cartela NF C 15-100
        val tbW = 280
        val tbH = 65
        val tbX = vbX + vbW - tbW - 10
        val tbY = vbY + vbH - tbH - 10
        sb.append("""
  <!-- Cartouche NF C 15-100 -->
  <g transform="translate($tbX, $tbY)">
    <rect class="title-block" width="$tbW" height="$tbH" rx="4"/>
    <text x="10" y="18" fill="$titleBlockTitle" font-family="sans-serif" font-size="11" font-weight="bold">⚡ ELEKTRON CAD NF C 15-100</text>
    <text x="10" y="34" fill="$titleBlockText" font-family="sans-serif" font-size="10">${escapeXml(doc.metadata.title)}</text>
    <text x="10" y="48" fill="$titleBlockSub" font-family="sans-serif" font-size="9">Auteur: ${escapeXml(doc.metadata.author)} | Date: ${escapeXml(doc.metadata.date)}</text>
  </g>
</svg>""")

        return sb.toString()
    }

    private fun renderSymbolSvg(sym: SymbolElement): String {
        val halfW = sym.width / 2f
        val halfH = sym.height / 2f
        val sb = StringBuilder()

        sb.append("  <g transform=\"translate(${sym.position.x}, ${sym.position.y}) rotate(${sym.rotationDegrees})\">\n")

        when (sym.type) {
            SymbolType.BUS_LINE_PH -> {
                sb.append("    <line x1=\"0\" y1=\"-$halfH\" x2=\"0\" y2=\"$halfH\" stroke=\"#E53935\" stroke-width=\"4\"/>\n")
                sb.append("    <circle cx=\"0\" cy=\"0\" r=\"4\" fill=\"#E53935\"/>\n")
            }
            SymbolType.BUS_LINE_N -> {
                sb.append("    <line x1=\"0\" y1=\"-$halfH\" x2=\"0\" y2=\"$halfH\" stroke=\"#1E88E5\" stroke-width=\"4\"/>\n")
                sb.append("    <circle cx=\"0\" cy=\"0\" r=\"4\" fill=\"#1E88E5\"/>\n")
            }
            SymbolType.BUS_LINE_PE -> {
                sb.append("    <line x1=\"0\" y1=\"-$halfH\" x2=\"0\" y2=\"$halfH\" stroke=\"#43A047\" stroke-width=\"4\"/>\n")
                sb.append("    <circle cx=\"0\" cy=\"0\" r=\"4\" fill=\"#43A047\"/>\n")
            }
            SymbolType.POLE_DISJONCTEUR_PH -> {
                sb.append("    <line class=\"symbol-line\" x1=\"-$halfW\" y1=\"0\" x2=\"-12\" y2=\"0\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-12\" y1=\"0\" x2=\"0\" y2=\"-8\"/>\n")
                sb.append("    <line stroke=\"#ECEFF1\" stroke-width=\"1.2\" x1=\"-8\" y1=\"-7\" x2=\"-4\" y2=\"-1\"/>\n")
                sb.append("    <line stroke=\"#ECEFF1\" stroke-width=\"1.2\" x1=\"-4\" y1=\"-7\" x2=\"-8\" y2=\"-1\"/>\n")
                sb.append("    <path class=\"symbol-line\" fill=\"none\" d=\"M 0,0 L 6,0 C 6,-7 12,-7 12,0 L $halfW,0\"/>\n")
            }
            SymbolType.POLE_DISJONCTEUR_N -> {
                sb.append("    <line class=\"symbol-line\" x1=\"-$halfW\" y1=\"0\" x2=\"-10\" y2=\"0\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-10\" y1=\"0\" x2=\"4\" y2=\"-8\"/>\n")
                sb.append("    <line stroke=\"#ECEFF1\" stroke-width=\"1.2\" x1=\"-6\" y1=\"-7\" x2=\"-2\" y2=\"-1\"/>\n")
                sb.append("    <line stroke=\"#ECEFF1\" stroke-width=\"1.2\" x1=\"-2\" y1=\"-7\" x2=\"-6\" y2=\"-1\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"4\" y1=\"0\" x2=\"$halfW\" y2=\"0\"/>\n")
            }
            SymbolType.CONTACT_INTERRUPTEUR -> {
                sb.append("    <line class=\"symbol-line\" x1=\"-$halfW\" y1=\"0\" x2=\"-8\" y2=\"0\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-8\" y1=\"0\" x2=\"8\" y2=\"-10\"/>\n")
                sb.append("    <circle cx=\"8\" cy=\"0\" r=\"2\" fill=\"#ECEFF1\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"8\" y1=\"0\" x2=\"$halfW\" y2=\"0\"/>\n")
            }
            SymbolType.LAMPE_AVEC_TERRE -> {
                val r = 12f
                val d = r * 0.707f
                sb.append("    <circle class=\"symbol-shape\" cx=\"0\" cy=\"0\" r=\"$r\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-$d\" y1=\"-$d\" x2=\"$d\" y2=\"$d\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-$d\" y1=\"$d\" x2=\"$d\" y2=\"-$d\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-$halfW\" y1=\"0\" x2=\"-$r\" y2=\"0\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"$r\" y1=\"0\" x2=\"$halfW\" y2=\"0\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"0\" y1=\"$r\" x2=\"0\" y2=\"${halfH - 4}\"/>\n")
                sb.append("    <path stroke=\"#43A047\" stroke-width=\"1.5\" fill=\"none\" d=\"M -6,${halfH - 4} A 6 6 0 0 0 6,${halfH - 4}\"/>\n")
            }
            SymbolType.DISJONCTEUR_MULTI -> {
                sb.append("    <rect class=\"symbol-shape\" x=\"-$halfW\" y=\"-$halfH\" width=\"${sym.width}\" height=\"${sym.height}\"/>\n")
                sb.append("    <circle cx=\"-12\" cy=\"-${halfH - 6}\" r=\"3\" fill=\"none\" stroke=\"#ECEFF1\" stroke-width=\"1.2\"/>\n")
                sb.append("    <circle cx=\"12\" cy=\"-${halfH - 6}\" r=\"3\" fill=\"none\" stroke=\"#ECEFF1\" stroke-width=\"1.2\"/>\n")
                sb.append("    <circle cx=\"-12\" cy=\"${halfH - 6}\" r=\"3\" fill=\"none\" stroke=\"#ECEFF1\" stroke-width=\"1.2\"/>\n")
                sb.append("    <circle cx=\"12\" cy=\"${halfH - 6}\" r=\"3\" fill=\"none\" stroke=\"#ECEFF1\" stroke-width=\"1.2\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-12\" y1=\"-${halfH - 9}\" x2=\"-12\" y2=\"-4\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-12\" y1=\"-4\" x2=\"-16\" y2=\"4\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-12\" y1=\"4\" x2=\"-12\" y2=\"${halfH - 9}\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"12\" y1=\"-${halfH - 9}\" x2=\"12\" y2=\"-4\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"12\" y1=\"-4\" x2=\"8\" y2=\"4\"/>\n")
                sb.append("    <path class=\"symbol-line\" fill=\"none\" d=\"M 12,4 L 15,6 L 9,8 L 12,10 L 12,${halfH - 9}\"/>\n")
            }
            SymbolType.DIFFERENTIEL_MULTI -> {
                sb.append("    <rect class=\"symbol-shape\" x=\"-$halfW\" y=\"-$halfH\" width=\"${sym.width}\" height=\"${sym.height}\"/>\n")
                sb.append("    <circle cx=\"-12\" cy=\"-${halfH - 6}\" r=\"3\" fill=\"none\" stroke=\"#ECEFF1\" stroke-width=\"1.2\"/>\n")
                sb.append("    <circle cx=\"12\" cy=\"-${halfH - 6}\" r=\"3\" fill=\"none\" stroke=\"#ECEFF1\" stroke-width=\"1.2\"/>\n")
                sb.append("    <circle cx=\"-12\" cy=\"${halfH - 6}\" r=\"3\" fill=\"none\" stroke=\"#ECEFF1\" stroke-width=\"1.2\"/>\n")
                sb.append("    <circle cx=\"12\" cy=\"${halfH - 6}\" r=\"3\" fill=\"none\" stroke=\"#ECEFF1\" stroke-width=\"1.2\"/>\n")
                sb.append("    <ellipse class=\"symbol-line\" cx=\"0\" cy=\"0\" rx=\"16\" ry=\"4\" fill=\"none\"/>\n")
            }
            SymbolType.INTERRUPTEUR_MURAL -> {
                sb.append("    <rect class=\"symbol-shape\" x=\"-$halfW\" y=\"-${halfH * 0.4}\" width=\"${sym.width}\" height=\"${sym.height * 0.8}\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-8\" y1=\"6\" x2=\"8\" y2=\"-4\"/>\n")
            }
            SymbolType.LAMPE_DCL_MULTI -> {
                val r = 10f
                val d = r * 0.707f
                sb.append("    <rect class=\"symbol-shape\" x=\"-${halfW * 0.4}\" y=\"-$halfH\" width=\"${sym.width * 0.4}\" height=\"${sym.height}\"/>\n")
                sb.append("    <rect class=\"symbol-shape\" x=\"-$halfW\" y=\"-${halfH * 0.4}\" width=\"${sym.width}\" height=\"${sym.height * 0.4}\"/>\n")
                sb.append("    <circle class=\"symbol-shape\" cx=\"0\" cy=\"0\" r=\"$r\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-$d\" y1=\"-$d\" x2=\"$d\" y2=\"$d\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-$d\" y1=\"$d\" x2=\"$d\" y2=\"-$d\"/>\n")
            }
            SymbolType.BORNE_WAGO -> {
                sb.append("    <rect class=\"symbol-shape\" x=\"-8\" y=\"-${halfH * 0.8}\" width=\"16\" height=\"${sym.height * 0.8}\"/>\n")
                sb.append("    <circle cx=\"0\" cy=\"-${halfH * 0.5}\" r=\"2.5\" fill=\"#00E5FF\"/>\n")
                sb.append("    <circle cx=\"0\" cy=\"0\" r=\"2.5\" fill=\"#00E5FF\"/>\n")
                sb.append("    <circle cx=\"0\" cy=\"${halfH * 0.5}\" r=\"2.5\" fill=\"#00E5FF\"/>\n")
            }
            SymbolType.BARRETTE_TERRE -> {
                sb.append("    <circle cx=\"0\" cy=\"-${halfH * 0.5}\" r=\"3.5\" stroke=\"#43A047\" fill=\"none\" stroke-width=\"1.5\"/>\n")
                sb.append("    <line x1=\"0\" y1=\"-${halfH * 0.5 - 3.5}\" x2=\"0\" y2=\"2\" stroke=\"#43A047\" stroke-width=\"2\"/>\n")
                sb.append("    <line x1=\"-12\" y1=\"2\" x2=\"12\" y2=\"2\" stroke=\"#43A047\" stroke-width=\"2.5\"/>\n")
                sb.append("    <line x1=\"-8\" y1=\"6\" x2=\"8\" y2=\"6\" stroke=\"#43A047\" stroke-width=\"2\"/>\n")
                sb.append("    <line x1=\"-4\" y1=\"10\" x2=\"4\" y2=\"10\" stroke=\"#43A047\" stroke-width=\"1.5\"/>\n")
            }
            SymbolType.CIRCUIT_BREAKER -> {
                sb.append("    <rect class=\"symbol-shape\" x=\"-$halfW\" y=\"-$halfH\" width=\"${sym.width}\" height=\"${sym.height}\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"0\" y1=\"-$halfH\" x2=\"0\" y2=\"-8\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"0\" y1=\"8\" x2=\"0\" y2=\"$halfH\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"0\" y1=\"8\" x2=\"-8\" y2=\"-6\"/>\n")
                sb.append("    <rect class=\"symbol-line\" x=\"-4\" y=\"-4\" width=\"8\" height=\"8\" fill=\"none\"/>\n")
            }
            SymbolType.DIFFERENTIAL -> {
                sb.append("    <rect class=\"symbol-shape\" x=\"-$halfW\" y=\"-$halfH\" width=\"${sym.width}\" height=\"${sym.height}\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"0\" y1=\"-$halfH\" x2=\"0\" y2=\"-8\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"0\" y1=\"8\" x2=\"0\" y2=\"$halfH\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"0\" y1=\"8\" x2=\"-8\" y2=\"-6\"/>\n")
                sb.append("    <ellipse class=\"symbol-line\" cx=\"0\" cy=\"0\" rx=\"12\" ry=\"6\" fill=\"none\"/>\n")
            }
            SymbolType.POWER_OUTLET -> {
                val r = halfW * 0.85f
                sb.append("    <path class=\"symbol-shape\" d=\"M -$r,4 A $r $r 0 0 1 $r,4 Z\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"0\" y1=\"4\" x2=\"0\" y2=\"-$halfH\"/>\n")
                sb.append("    <circle cx=\"-6\" cy=\"-2\" r=\"2\" fill=\"#ECEFF1\"/>\n")
                sb.append("    <circle cx=\"6\" cy=\"-2\" r=\"2\" fill=\"#ECEFF1\"/>\n")
            }
            SymbolType.LIGHT_POINT -> {
                val r = halfW * 0.75f
                val d = r * 0.707f
                sb.append("    <circle class=\"symbol-shape\" cx=\"0\" cy=\"0\" r=\"$r\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-$d\" y1=\"-$d\" x2=\"$d\" y2=\"$d\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"-$d\" y1=\"$d\" x2=\"$d\" y2=\"-$d\"/>\n")
            }
            SymbolType.SWITCH_SIMPLE -> {
                val r = halfW * 0.5f
                sb.append("    <circle class=\"symbol-shape\" cx=\"0\" cy=\"0\" r=\"$r\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"0\" y1=\"0\" x2=\"${halfW * 0.8f}\" y2=\"-${halfH * 0.8f}\"/>\n")
                sb.append("    <line class=\"symbol-line\" x1=\"${halfW * 0.8f}\" y1=\"-${halfH * 0.8f}\" x2=\"${halfW * 0.8f}\" y2=\"-${halfH * 0.4f}\"/>\n")
            }
        }

        // Bornas de conexión
        for (term in sym.getTerminals()) {
            sb.append("    <circle class=\"terminal\" cx=\"${term.relativeOffset.x}\" cy=\"${term.relativeOffset.y}\" r=\"3.5\"/>\n")
        }

        sb.append("  </g>\n")

        sb.append("  <text class=\"label-title\" x=\"${sym.position.x - halfW}\" y=\"${sym.position.y + halfH + 14}\">${escapeXml(sym.label)}</text>\n")
        sb.append("  <text class=\"label-sub\" x=\"${sym.position.x - halfW}\" y=\"${sym.position.y + halfH + 24}\">${escapeXml(sym.designation)}</text>\n")

        return sb.toString()
    }

    private fun colorToHex(color: Color): String {
        val r = (color.red * 255).toInt().coerceIn(0, 255).toString(16).padStart(2, '0')
        val g = (color.green * 255).toInt().coerceIn(0, 255).toString(16).padStart(2, '0')
        val b = (color.blue * 255).toInt().coerceIn(0, 255).toString(16).padStart(2, '0')
        return "#$r$g$b"
    }

    private fun escapeXml(s: String): String =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}
