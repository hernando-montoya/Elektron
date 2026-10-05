package org.nicokosi.elektron.domain.export

import androidx.compose.ui.geometry.Offset
import org.nicokosi.elektron.domain.model.GraphicElement

/**
 * Metadatos descriptivos de la instalación eléctrica.
 */
data class ProjectMetadata(
    val title: String = "Installation Résidentielle NF C 15-100",
    val author: String = "Électricien Certifié",
    val date: String = "2026-10-05",
    val standardVersion: String = "NF C 15-100 / A5"
)

/**
 * Estructura completa de un archivo de proyecto de Elektron CAD (.elektron).
 */
data class ProjectDocument(
    val version: Int = 1,
    val metadata: ProjectMetadata = ProjectMetadata(),
    val panX: Float = 0f,
    val panY: Float = 0f,
    val scale: Float = 1.0f,
    val gridSize: Float = 20f,
    val elements: List<GraphicElement> = emptyList()
)
