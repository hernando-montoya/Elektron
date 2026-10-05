package org.nicokosi.elektron.domain.export

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.nicokosi.elektron.domain.model.*

/**
 * Serializador y deserializador JSON ligero y robusto para proyectos Elektron CAD.
 */
object ProjectSerializer {

    fun toJson(doc: ProjectDocument): String {
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": ${doc.version},\n")
        sb.append("  \"metadata\": {\n")
        sb.append("    \"title\": \"${escapeJson(doc.metadata.title)}\",\n")
        sb.append("    \"author\": \"${escapeJson(doc.metadata.author)}\",\n")
        sb.append("    \"date\": \"${escapeJson(doc.metadata.date)}\",\n")
        sb.append("    \"standard\": \"${escapeJson(doc.metadata.standardVersion)}\"\n")
        sb.append("  },\n")
        sb.append("  \"viewport\": {\n")
        sb.append("    \"panX\": ${doc.panX},\n")
        sb.append("    \"panY\": ${doc.panY},\n")
        sb.append("    \"scale\": ${doc.scale},\n")
        sb.append("    \"gridSize\": ${doc.gridSize}\n")
        sb.append("  },\n")
        sb.append("  \"elements\": [\n")

        val elemStrings = doc.elements.map { el ->
            when (el) {
                is WireElement -> serializeWire(el)
                is BoxElement -> serializeBox(el)
                is SymbolElement -> serializeSymbol(el)
                is TextElement -> serializeText(el)
            }
        }
        sb.append(elemStrings.joinToString(",\n"))
        sb.append("\n  ]\n")
        sb.append("}")
        return sb.toString()
    }

    private fun serializeText(text: TextElement): String {
        return """    {
      "kind": "TEXT",
      "id": "${text.id}",
      "text": "${escapeJson(text.text)}",
      "position": [${text.position.x}, ${text.position.y}],
      "fontSize": ${text.fontSize},
      "isBold": ${text.isBold}
    }"""
    }

    private fun serializeWire(wire: WireElement): String {
        val ptsJson = wire.points.joinToString(", ") { "[${it.x}, ${it.y}]" }
        val startAnchorJson = wire.startAnchor?.let { "{\"el\":\"${it.elementId}\", \"term\":\"${it.terminalId}\"}" } ?: "null"
        val endAnchorJson = wire.endAnchor?.let { "{\"el\":\"${it.elementId}\", \"term\":\"${it.terminalId}\"}" } ?: "null"
        return """    {
      "kind": "WIRE",
      "id": "${wire.id}",
      "points": [$ptsJson],
      "strokeWidth": ${wire.strokeWidth},
      "startAnchor": $startAnchorJson,
      "endAnchor": $endAnchorJson
    }"""
    }

    private fun serializeBox(box: BoxElement): String {
        return """    {
      "kind": "BOX",
      "id": "${box.id}",
      "topLeft": [${box.topLeft.x}, ${box.topLeft.y}],
      "bottomRight": [${box.bottomRight.x}, ${box.bottomRight.y}],
      "strokeWidth": ${box.strokeWidth}
    }"""
    }

    private fun serializeSymbol(sym: SymbolElement): String {
        return """    {
      "kind": "SYMBOL",
      "id": "${sym.id}",
      "type": "${sym.type.name}",
      "position": [${sym.position.x}, ${sym.position.y}],
      "rotation": ${sym.rotationDegrees},
      "label": "${escapeJson(sym.label)}",
      "designation": "${escapeJson(sym.designation)}"
    }"""
    }

    /**
     * Parsea un texto JSON simple producido por el exportador.
     */
    fun fromJson(json: String): ProjectDocument? {
        try {
            val title = extractString(json, "\"title\"") ?: "Installation NF C 15-100"
            val author = extractString(json, "\"author\"") ?: "Électricien"
            val date = extractString(json, "\"date\"") ?: "2026-10-05"
            val standard = extractString(json, "\"standard\"") ?: "NF C 15-100"

            val panX = extractFloat(json, "\"panX\"") ?: 0f
            val panY = extractFloat(json, "\"panY\"") ?: 0f
            val scale = extractFloat(json, "\"scale\"") ?: 1.0f
            val gridSize = extractFloat(json, "\"gridSize\"") ?: 20f

            val elements = mutableListOf<GraphicElement>()

            // Dividir bloques de elementos por "kind"
            val kindBlocks = json.split("{\"kind\"")
            for (i in 1 until kindBlocks.size) {
                val block = "{\"kind\"" + kindBlocks[i].substringBefore("}") + "}"
                val kind = extractString(block, "\"kind\"")
                val id = extractString(block, "\"id\"") ?: "elem-$i"

                when (kind) {
                    "WIRE" -> {
                        val points = extractPointsList(kindBlocks[i])
                        if (points.size >= 2) {
                            elements.add(
                                WireElement(
                                    id = id,
                                    points = points
                                )
                            )
                        }
                    }
                    "BOX" -> {
                        val tl = extractPoint(block, "\"topLeft\"") ?: Offset.Zero
                        val br = extractPoint(block, "\"bottomRight\"") ?: Offset(100f, 100f)
                        elements.add(
                            BoxElement(
                                id = id,
                                topLeft = tl,
                                bottomRight = br
                            )
                        )
                    }
                    "SYMBOL" -> {
                        val typeStr = extractString(block, "\"type\"") ?: SymbolType.CIRCUIT_BREAKER.name
                        val type = runCatching { SymbolType.valueOf(typeStr) }.getOrDefault(SymbolType.CIRCUIT_BREAKER)
                        val pos = extractPoint(block, "\"position\"") ?: Offset.Zero
                        val rot = extractInt(block, "\"rotation\"") ?: 0
                        val label = extractString(block, "\"label\"") ?: type.defaultLabel
                        val des = extractString(block, "\"designation\"") ?: type.standardRating

                        elements.add(
                            SymbolElement(
                                id = id,
                                type = type,
                                position = pos,
                                rotationDegrees = rot,
                                label = label,
                                designation = des
                            )
                        )
                    }
                    "TEXT" -> {
                        val text = extractString(block, "\"text\"") ?: "Texte"
                        val pos = extractPoint(block, "\"position\"") ?: Offset.Zero
                        val fontSize = extractFloat(block, "\"fontSize\"") ?: 13f
                        val isBold = block.contains("\"isBold\": true") || block.contains("\"isBold\":true")
                        elements.add(
                            TextElement(
                                id = id,
                                text = text,
                                position = pos,
                                fontSize = fontSize,
                                isBold = isBold
                            )
                        )
                    }
                }
            }

            return ProjectDocument(
                metadata = ProjectMetadata(title, author, date, standard),
                panX = panX,
                panY = panY,
                scale = scale,
                gridSize = gridSize,
                elements = elements
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun extractString(json: String, key: String): String? {
        val idx = json.indexOf(key)
        if (idx == -1) return null
        val colon = json.indexOf(':', idx)
        if (colon == -1) return null
        val quoteStart = json.indexOf('"', colon)
        if (quoteStart == -1) return null
        val quoteEnd = json.indexOf('"', quoteStart + 1)
        if (quoteEnd == -1) return null
        return json.substring(quoteStart + 1, quoteEnd)
    }

    private fun extractFloat(json: String, key: String): Float? {
        val idx = json.indexOf(key)
        if (idx == -1) return null
        val colon = json.indexOf(':', idx)
        if (colon == -1) return null
        val part = json.substring(colon + 1).trimStart().takeWhile { it.isDigit() || it == '.' || it == '-' }
        return part.toFloatOrNull()
    }

    private fun extractInt(json: String, key: String): Int? {
        val idx = json.indexOf(key)
        if (idx == -1) return null
        val colon = json.indexOf(':', idx)
        if (colon == -1) return null
        val part = json.substring(colon + 1).trimStart().takeWhile { it.isDigit() || it == '-' }
        return part.toIntOrNull()
    }

    private fun extractPoint(json: String, key: String): Offset? {
        val idx = json.indexOf(key)
        if (idx == -1) return null
        val openBracket = json.indexOf('[', idx)
        if (openBracket == -1) return null
        val closeBracket = json.indexOf(']', openBracket)
        if (closeBracket == -1) return null
        val coords = json.substring(openBracket + 1, closeBracket).split(',')
        if (coords.size == 2) {
            val x = coords[0].trim().toFloatOrNull() ?: 0f
            val y = coords[1].trim().toFloatOrNull() ?: 0f
            return Offset(x, y)
        }
        return null
    }

    private fun extractPointsList(rawBlock: String): List<Offset> {
        val points = mutableListOf<Offset>()
        val startIdx = rawBlock.indexOf("\"points\": [")
        if (startIdx == -1) return emptyList()
        val endIdx = rawBlock.indexOf(']', startIdx + 11)
        // Buscar pares [x, y]
        var searchPos = startIdx + 10
        while (searchPos < rawBlock.length && searchPos < endIdx + 200) {
            val ob = rawBlock.indexOf('[', searchPos)
            if (ob == -1 || ob > endIdx) break
            val cb = rawBlock.indexOf(']', ob)
            if (cb == -1) break
            val parts = rawBlock.substring(ob + 1, cb).split(',')
            if (parts.size == 2) {
                val x = parts[0].trim().toFloatOrNull()
                val y = parts[1].trim().toFloatOrNull()
                if (x != null && y != null) {
                    points.add(Offset(x, y))
                }
            }
            searchPos = cb + 1
        }
        return points
    }
}
