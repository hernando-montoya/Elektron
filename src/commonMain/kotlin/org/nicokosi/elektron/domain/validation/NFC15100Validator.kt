package org.nicokosi.elektron.domain.validation

import org.nicokosi.elektron.domain.model.CircuitGraph
import org.nicokosi.elektron.domain.model.GraphicElement
import org.nicokosi.elektron.domain.model.SymbolElement
import org.nicokosi.elektron.domain.model.SymbolType

/**
 * Nivel de severidad de la regla según la norma francesa NF C 15-100.
 */
enum class IssueSeverity {
    ERROR,   // No conforme (incumplimiento normativo estricto)
    WARNING, // Riesgo técnico o advertencia de diseño
    INFO     // Información técnica / recomendación
}

/**
 * Representa un diagnóstico o aviso normativo emitido por el validador.
 */
data class ValidationIssue(
    val id: String,
    val ruleCode: String,
    val title: String,
    val description: String,
    val severity: IssueSeverity,
    val relatedElementIds: List<String> = emptyList(),
    val standardReference: String = "NF C 15-100 § 771"
)

/**
 * Resumen global del análisis de conformidad normativa.
 */
data class ComplianceReport(
    val issues: List<ValidationIssue>,
    val totalCircuits: Int,
    val compliantCircuits: Int
) {
    val errorCount: Int get() = issues.count { it.severity == IssueSeverity.ERROR }
    val warningCount: Int get() = issues.count { it.severity == IssueSeverity.WARNING }
    val isFullyCompliant: Boolean get() = errorCount == 0 && totalCircuits > 0
}

/**
 * Motor de validación exhaustivo según la norma NF C 15-100 para esquemas unifilares, desarrollados y multifilares.
 */
object NFC15100Validator {

    fun validate(elements: List<GraphicElement>): ComplianceReport {
        val symbols = elements.filterIsInstance<SymbolElement>()
        val nets = CircuitGraph.buildNets(elements)
        val issues = mutableListOf<ValidationIssue>()

        var issueIdCounter = 1
        fun nextIssueId(): String = "ISSUE-${issueIdCounter++}"

        if (symbols.isEmpty()) {
            return ComplianceReport(emptyList(), 0, 0)
        }

        // ==============================================================
        // REGLA 1: Presencia de protección diferencial 30mA
        // ==============================================================
        val hasDifferential = symbols.any {
            it.type == SymbolType.DIFFERENTIAL || it.type == SymbolType.DIFFERENTIEL_MULTI
        }
        val hasLoads = symbols.any {
            it.type == SymbolType.POWER_OUTLET ||
            it.type == SymbolType.LIGHT_POINT ||
            it.type == SymbolType.LAMPE_AVEC_TERRE ||
            it.type == SymbolType.LAMPE_DCL_MULTI
        }

        if (!hasDifferential && hasLoads) {
            issues.add(
                ValidationIssue(
                    id = nextIssueId(),
                    ruleCode = "NF-DDR-30MA",
                    title = "Protection différentielle 30mA absente",
                    description = "Tous les circuits terminaux (prises et éclairage) doivent être placés sous la protection d'un différentiel 30 mA.",
                    severity = IssueSeverity.ERROR,
                    standardReference = "NF C 15-100 § 771.531.2.3.2"
                )
            )
        }

        var compliantCircuitsCount = 0

        // ==============================================================
        // REGLAS 2 y 3: Análisis individual de cada circuito/red eléctrica
        // ==============================================================
        for (net in nets) {
            val netSymbols = symbols.filter { it.id in net.connectedSymbolIds }
            val outlets = netSymbols.filter { it.type == SymbolType.POWER_OUTLET }
            val lights = netSymbols.filter {
                it.type == SymbolType.LIGHT_POINT ||
                it.type == SymbolType.LAMPE_AVEC_TERRE ||
                it.type == SymbolType.LAMPE_DCL_MULTI
            }
            val breakers = netSymbols.filter {
                it.type == SymbolType.CIRCUIT_BREAKER ||
                it.type == SymbolType.POLE_DISJONCTEUR_PH ||
                it.type == SymbolType.DISJONCTEUR_MULTI
            }

            var hasCircuitError = false

            // 1. Detección de circuito huérfano (cargas sin disyuntor asignado en la red)
            if ((outlets.isNotEmpty() || lights.isNotEmpty()) && breakers.isEmpty()) {
                hasCircuitError = true
                issues.add(
                    ValidationIssue(
                        id = nextIssueId(),
                        ruleCode = "NF-NO-BREAKER",
                        title = "Circuit ${net.id} sans disjoncteur dédié",
                        description = "Les récepteurs sont câblés sans disjoncteur magnétothermique Ph+N en amont.",
                        severity = IssueSeverity.ERROR,
                        relatedElementIds = netSymbols.map { it.id },
                        standardReference = "NF C 15-100 § 771.533.2"
                    )
                )
            }

            // 2. Límite de tomas de corriente por circuito
            val outletCount = outlets.size
            if (outletCount > 12) {
                hasCircuitError = true
                issues.add(
                    ValidationIssue(
                        id = nextIssueId(),
                        ruleCode = "NF-OUTLET-LIMIT-MAX",
                        title = "Dépassement du nombre maximal de prises (${net.id})",
                        description = "Ce circuit comporte $outletCount prises. Le maximum absolu sous 2,5 mm² / 20A est de 12 socles de prise.",
                        severity = IssueSeverity.ERROR,
                        relatedElementIds = outlets.map { it.id },
                        standardReference = "NF C 15-100 Tableau 771F"
                    )
                )
            } else if (outletCount > 8) {
                issues.add(
                    ValidationIssue(
                        id = nextIssueId(),
                        ruleCode = "NF-OUTLET-SECTION-REQ",
                        title = "Section de câble 2,5 mm² requise (${net.id})",
                        description = "Ce circuit compte $outletCount prises (> 8). Une section minimale de 2,5 mm² et disjoncteur 20A max sont obligatoires.",
                        severity = IssueSeverity.WARNING,
                        relatedElementIds = outlets.map { it.id },
                        standardReference = "NF C 15-100 Tableau 771F"
                    )
                )
            }

            // 3. Límite de puntos de iluminación por circuito
            val lightCount = lights.size
            if (lightCount > 8) {
                hasCircuitError = true
                issues.add(
                    ValidationIssue(
                        id = nextIssueId(),
                        ruleCode = "NF-LIGHT-LIMIT-MAX",
                        title = "Dépassement de points lumineux (${net.id})",
                        description = "Ce circuit comporte $lightCount points d'éclairage. La norme autorise un maximum de 8 points par circuit (section 1,5 mm² / 16A max).",
                        severity = IssueSeverity.ERROR,
                        relatedElementIds = lights.map { it.id },
                        standardReference = "NF C 15-100 § 771.314.2.3"
                    )
                )
            }

            // 4. Mezcla tomas y luces
            if (outlets.isNotEmpty() && lights.isNotEmpty()) {
                issues.add(
                    ValidationIssue(
                        id = nextIssueId(),
                        ruleCode = "NF-CIRCUIT-SEPARATION",
                        title = "Mélange prises et éclairage détecté (${net.id})",
                        description = "Les circuits d'éclairage et de prises de courant doivent être distincts et alimentés par des départs séparés.",
                        severity = IssueSeverity.WARNING,
                        relatedElementIds = (outlets + lights).map { it.id },
                        standardReference = "NF C 15-100 § 771.314.2.1"
                    )
                )
            }

            if (!hasCircuitError && (outlets.isNotEmpty() || lights.isNotEmpty())) {
                compliantCircuitsCount++
            }
        }

        return ComplianceReport(
            issues = issues,
            totalCircuits = nets.size,
            compliantCircuits = compliantCircuitsCount
        )
    }
}
