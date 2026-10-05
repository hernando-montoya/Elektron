package org.nicokosi.elektron.domain.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.abs

/**
 * Representa una red equipotencial o circuito cerrado/abierto entre componentes interconectados.
 */
data class ElectricNet(
    val id: String,
    val wireIds: Set<String>,
    val connectedSymbolIds: Set<String>,
    val junctionPoints: List<Offset>
)

/**
 * Motor de análisis topológico y grafo eléctrico del esquema.
 */
object CircuitGraph {

    /**
     * Calcula los puntos de unión (junction dots) donde dos o más cables convergen
     * o un cable se conecta a la borna de un símbolo o al extremo de otro cable.
     */
    fun findJunctionPoints(
        wires: List<WireElement>,
        symbols: List<SymbolElement>,
        tolerance: Float = 4.0f
    ): List<Offset> {
        val junctions = mutableListOf<Offset>()
        val endpoints = mutableListOf<Offset>()

        // 1. Recolectar todos los extremos y vértices de los cables
        for (wire in wires) {
            endpoints.addAll(wire.points)
        }

        // 2. Comprobar coincidencias de puntos entre diferentes cables
        for (i in 0 until endpoints.size) {
            val p1 = endpoints[i]
            var count = 0
            for (j in 0 until endpoints.size) {
                if (i != j && (p1 - endpoints[j]).getDistance() <= tolerance) {
                    count++
                }
            }
            // Si al menos 2 conexiones convergen en este punto, es una unión física
            if (count >= 2) {
                if (junctions.none { (it - p1).getDistance() <= tolerance }) {
                    junctions.add(p1)
                }
            }
        }

        // 3. Comprobar extremos de cables que tocan un segmento de otro cable (derivación en T)
        for (wire in wires) {
            val otherWires = wires.filter { it.id != wire.id }
            for (pt in listOf(wire.startPoint, wire.endPoint)) {
                for (other in otherWires) {
                    if (other.hits(pt, tolerance)) {
                        if (junctions.none { (it - pt).getDistance() <= tolerance }) {
                            junctions.add(pt)
                        }
                    }
                }
            }
        }

        return junctions
    }

    /**
     * Busca la borna más cercana a un punto dado dentro de un radio de tolerancia.
     * Útil para snapping automático a terminales.
     */
    fun findNearestTerminal(
        point: Offset,
        symbols: List<SymbolElement>,
        maxDistance: Float = 14.0f
    ): Pair<SymbolElement, Pair<Terminal, Offset>>? {
        var nearest: Pair<SymbolElement, Pair<Terminal, Offset>>? = null
        var minDistance = maxDistance

        for (sym in symbols) {
            for (termPair in sym.getAbsoluteTerminals()) {
                val dist = (point - termPair.second).getDistance()
                if (dist <= minDistance) {
                    minDistance = dist
                    nearest = Pair(sym, termPair)
                }
            }
        }
        return nearest
    }

    /**
     * Construye las redes eléctricas (Nets) agrupando cables que comparten nodos o bornas.
     */
    fun buildNets(elements: List<GraphicElement>): List<ElectricNet> {
        val wires = elements.filterIsInstance<WireElement>()
        val symbols = elements.filterIsInstance<SymbolElement>()
        val visitedWires = mutableSetOf<String>()
        val nets = mutableListOf<ElectricNet>()
        var netCounter = 1

        val junctions = findJunctionPoints(wires, symbols)

        for (wire in wires) {
            if (wire.id in visitedWires) continue

            val currentNetWires = mutableSetOf<String>()
            val currentNetSymbols = mutableSetOf<String>()
            val queue = ArrayDeque<WireElement>()

            queue.add(wire)
            visitedWires.add(wire.id)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                currentNetWires.add(current.id)

                // Buscar símbolos conectados a este cable
                current.startAnchor?.let { currentNetSymbols.add(it.elementId) }
                current.endAnchor?.let { currentNetSymbols.add(it.elementId) }

                // Buscar cables conectados físicamente a este
                for (other in wires) {
                    if (other.id !in visitedWires) {
                        val connects = (current.startPoint - other.startPoint).getDistance() <= 4f ||
                                       (current.startPoint - other.endPoint).getDistance() <= 4f ||
                                       (current.endPoint - other.startPoint).getDistance() <= 4f ||
                                       (current.endPoint - other.endPoint).getDistance() <= 4f

                        if (connects) {
                            visitedWires.add(other.id)
                            queue.add(other)
                        }
                    }
                }
            }

            nets.add(
                ElectricNet(
                    id = "NET-${netCounter++}",
                    wireIds = currentNetWires,
                    connectedSymbolIds = currentNetSymbols,
                    junctionPoints = junctions.filter { j ->
                        wires.filter { it.id in currentNetWires }.any { it.hits(j, 4f) }
                    }
                )
            )
        }

        return nets
    }
}
