package org.nicokosi.elektron.platform

import androidx.compose.runtime.compositionLocalOf

/**
 * CompositionLocal para acceder a las capacidades de almacenamiento y descarga de la plataforma.
 */
val LocalPlatformStorage = compositionLocalOf<PlatformStorage?> { null }
