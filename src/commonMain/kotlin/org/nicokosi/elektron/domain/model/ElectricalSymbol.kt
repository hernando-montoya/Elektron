package org.nicokosi.elektron.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Categorías de componentes según normativa NF C 15-100 y tipología de esquema.
 */
enum class SymbolCategory(val displayName: String) {
    SCHEMA_DEVELOPPE("Schéma Développé"),
    SCHEMA_MULTIFILAIRE("Schéma Multifilaire"),
    PROTECTION("Tableau & Protection"),
    OUTLETS("Prises de courant"),
    LIGHTING_SWITCHES("Éclairage & Commande")
}

/**
 * Tipos específicos de aparamenta eléctrica para esquemas desarrollados, multifilares y unifilares.
 */
enum class SymbolType(
    val category: SymbolCategory,
    val defaultLabel: String,
    val description: String,
    val standardRating: String
) {
    // ==========================================
    // COMPONENTES SCHÉMA DÉVELOPPÉ (IMAGEN 1)
    // ==========================================
    BUS_LINE_PH(
        SymbolCategory.SCHEMA_DEVELOPPE,
        "PH",
        "Ligne d'alimentation Phase (Barre PH)",
        "230V AC"
    ),
    BUS_LINE_N(
        SymbolCategory.SCHEMA_DEVELOPPE,
        "N",
        "Ligne d'alimentation Neutre (Barre N)",
        "0V"
    ),
    BUS_LINE_PE(
        SymbolCategory.SCHEMA_DEVELOPPE,
        "PE",
        "Ligne de Terre Équipotentielle (Barre PE)",
        "Terre"
    ),
    POLE_DISJONCTEUR_PH(
        SymbolCategory.SCHEMA_DEVELOPPE,
        "Q1",
        "Pôle Phase de disjoncteur (Magnétothermique)",
        "10A / 16A"
    ),
    POLE_DISJONCTEUR_N(
        SymbolCategory.SCHEMA_DEVELOPPE,
        "Q1",
        "Pôle Neutre de disjoncteur (Sectionneur)",
        "Neutre coupé"
    ),
    CONTACT_INTERRUPTEUR(
        SymbolCategory.SCHEMA_DEVELOPPE,
        "S1",
        "Contact d'interrupteur simple allumage",
        "10AX"
    ),
    LAMPE_AVEC_TERRE(
        SymbolCategory.SCHEMA_DEVELOPPE,
        "L1",
        "Lampe / Récepteur avec borne de terre PE",
        "230V"
    ),

    // ==========================================
    // COMPONENTES SCHÉMA MULTIFILAIRE (IMAGEN 2)
    // ==========================================
    DISJONCTEUR_MULTI(
        SymbolCategory.SCHEMA_MULTIFILAIRE,
        "Q1",
        "Disjoncteur Ph+N multifilaire à bornes",
        "16A / 3kA"
    ),
    DIFFERENTIEL_MULTI(
        SymbolCategory.SCHEMA_MULTIFILAIRE,
        "Q2",
        "Interrupteur différentiel 30mA multifilaire",
        "40A - 30mA"
    ),
    INTERRUPTEUR_MURAL(
        SymbolCategory.SCHEMA_MULTIFILAIRE,
        "S1",
        "Interrupteur en boîte d'encastrement",
        "10AX"
    ),
    LAMPE_DCL_MULTI(
        SymbolCategory.SCHEMA_MULTIFILAIRE,
        "L1",
        "Point lumineux DCL avec enveloppe",
        "10A max"
    ),
    BORNE_WAGO(
        SymbolCategory.SCHEMA_MULTIFILAIRE,
        "Wago",
        "Borne de connexion rapide pour boîte de dérivation",
        "32A"
    ),
    BARRETTE_TERRE(
        SymbolCategory.SCHEMA_MULTIFILAIRE,
        "Terre",
        "Barrette collectrice de terre du tableau",
        "PE"
    ),

    // ==========================================
    // COMPONENTES GENERALES Y UNIFILARES
    // ==========================================
    CIRCUIT_BREAKER(
        SymbolCategory.PROTECTION,
        "Disjoncteur",
        "Disjoncteur magnétothermique Ph+N modulaire",
        "16A / 3000A"
    ),
    DIFFERENTIAL(
        SymbolCategory.PROTECTION,
        "Différentiel",
        "Interrupteur différentiel 30mA Type A/AC",
        "40A - 30mA"
    ),
    POWER_OUTLET(
        SymbolCategory.OUTLETS,
        "Prise 2P+T",
        "Socle de prise de courant 16A 2P+T",
        "16A - 230V"
    ),
    LIGHT_POINT(
        SymbolCategory.LIGHTING_SWITCHES,
        "Point lumineux",
        "Point d'éclairage unifilaire DCL",
        "10A max"
    ),
    SWITCH_SIMPLE(
        SymbolCategory.LIGHTING_SWITCHES,
        "Simple allumage",
        "Interrupteur à commande manuelle",
        "10AX - 230V"
    )
}

/**
 * Bornas de conexión física de un componente.
 */
data class Terminal(
    val id: String,
    val name: String,
    val relativeOffset: Offset
)

/**
 * Elemento gráfico de un componente eléctrico normalizado NF C 15-100.
 */
data class SymbolElement(
    override val id: String,
    val type: SymbolType,
    val position: Offset,
    val rotationDegrees: Int = 0,
    val label: String = type.defaultLabel,
    val designation: String = type.standardRating,
    val width: Float = 40f,
    val height: Float = 40f,
    override val isSelected: Boolean = false
) : GraphicElement {

    override fun copyWithSelected(selected: Boolean): GraphicElement =
        copy(isSelected = selected)

    fun rotateClockwise(): SymbolElement =
        copy(rotationDegrees = (rotationDegrees + 90) % 360)

    /**
     * Bornas definidas según el tipo de símbolo y su geometría.
     */
    fun getTerminals(): List<Terminal> {
        val halfW = width / 2f
        val halfH = height / 2f
        return when (type) {
            // Líneas de bus para Schéma Développé (PH, N, PE)
            SymbolType.BUS_LINE_PH, SymbolType.BUS_LINE_N, SymbolType.BUS_LINE_PE -> listOf(
                Terminal("top", "Haut", Offset(0f, -halfH)),
                Terminal("mid", "Connexion", Offset(0f, 0f)),
                Terminal("bottom", "Bas", Offset(0f, halfH))
            )

            // Polo Disyuntor Fase Développé (horizontal)
            SymbolType.POLE_DISJONCTEUR_PH -> listOf(
                Terminal("in", "1", Offset(-halfW, 0f)),
                Terminal("out", "2", Offset(halfW, 0f))
            )

            // Polo Disyuntor Neutro Développé (horizontal)
            SymbolType.POLE_DISJONCTEUR_N -> listOf(
                Terminal("in", "N", Offset(-halfW, 0f)),
                Terminal("out", "N'", Offset(halfW, 0f))
            )

            // Contacto de interruptor Développé (horizontal)
            SymbolType.CONTACT_INTERRUPTEUR -> listOf(
                Terminal("in", "1", Offset(-halfW, 0f)),
                Terminal("out", "2", Offset(halfW, 0f))
            )

            // Lámpara con borna de tierra Développé
            SymbolType.LAMPE_AVEC_TERRE -> listOf(
                Terminal("in", "L", Offset(-halfW, 0f)),
                Terminal("out", "N", Offset(halfW, 0f)),
                Terminal("pe", "PE", Offset(0f, halfH))
            )

            // Disjoncteur Ph+N Multifilaire con 4 bornas circulares
            SymbolType.DISJONCTEUR_MULTI -> listOf(
                Terminal("in_ph", "1", Offset(-12f, -halfH)),
                Terminal("in_n", "N", Offset(12f, -halfH)),
                Terminal("out_ph", "2", Offset(-12f, halfH)),
                Terminal("out_n", "N'", Offset(12f, halfH))
            )

            // Différentiel Multifilaire con 4 bornas circulares
            SymbolType.DIFFERENTIEL_MULTI -> listOf(
                Terminal("in_ph", "1", Offset(-12f, -halfH)),
                Terminal("in_n", "N", Offset(12f, -halfH)),
                Terminal("out_ph", "2", Offset(-12f, halfH)),
                Terminal("out_n", "N'", Offset(12f, halfH))
            )

            // Interrupteur mural en caja de empotrar
            SymbolType.INTERRUPTEUR_MURAL -> listOf(
                Terminal("l", "L", Offset(-10f, -halfH)),
                Terminal("1", "1", Offset(10f, -halfH))
            )

            // Lampe DCL multifilaire avec enveloppe
            SymbolType.LAMPE_DCL_MULTI -> listOf(
                Terminal("ph", "L", Offset(-12f, -halfH)),
                Terminal("n", "N", Offset(0f, -halfH)),
                Terminal("pe", "PE", Offset(12f, -halfH))
            )

            // Borne Wago de derivación
            SymbolType.BORNE_WAGO -> listOf(
                Terminal("1", "1", Offset(0f, -halfH * 0.7f)),
                Terminal("2", "2", Offset(0f, 0f)),
                Terminal("3", "3", Offset(0f, halfH * 0.7f))
            )

            // Barrette de terre
            SymbolType.BARRETTE_TERRE -> listOf(
                Terminal("in", "PE_In", Offset(0f, -halfH * 0.5f)),
                Terminal("out", "PE_Out", Offset(0f, halfH * 0.5f))
            )

            SymbolType.CIRCUIT_BREAKER, SymbolType.DIFFERENTIAL -> listOf(
                Terminal("in_phase", "L In", Offset(-10f, -halfH)),
                Terminal("in_neutral", "N In", Offset(10f, -halfH)),
                Terminal("out_phase", "L Out", Offset(-10f, halfH)),
                Terminal("out_neutral", "N Out", Offset(10f, halfH))
            )
            SymbolType.POWER_OUTLET -> listOf(
                Terminal("phase", "L", Offset(-halfW, 0f)),
                Terminal("neutral", "N", Offset(halfW, 0f)),
                Terminal("earth", "PE", Offset(0f, -halfH))
            )
            SymbolType.LIGHT_POINT -> listOf(
                Terminal("in", "1", Offset(-halfW, 0f)),
                Terminal("out", "2", Offset(halfW, 0f))
            )
            SymbolType.SWITCH_SIMPLE -> listOf(
                Terminal("line", "L", Offset(0f, -halfH)),
                Terminal("lamp", "1", Offset(0f, halfH))
            )
        }
    }

    fun getAbsoluteTerminals(): List<Pair<Terminal, Offset>> {
        val rad = (rotationDegrees * PI / 180.0).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)

        return getTerminals().map { terminal ->
            val rx = terminal.relativeOffset.x * cosA - terminal.relativeOffset.y * sinA
            val ry = terminal.relativeOffset.x * sinA + terminal.relativeOffset.y * cosA
            terminal to (position + Offset(rx, ry))
        }
    }

    override fun bounds(): Rect {
        val halfW = width / 2f
        val halfH = height / 2f
        return Rect(
            left = position.x - halfW,
            top = position.y - halfH,
            right = position.x + halfW,
            bottom = position.y + halfH
        )
    }

    override fun hits(point: Offset, tolerance: Float): Boolean {
        val b = bounds()
        return point.x in (b.left - tolerance)..(b.right + tolerance) &&
               point.y in (b.top - tolerance)..(b.bottom + tolerance)
    }

    override fun translate(delta: Offset): GraphicElement =
        copy(position = position + delta)
}
