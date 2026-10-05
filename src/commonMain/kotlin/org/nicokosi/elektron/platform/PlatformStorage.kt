package org.nicokosi.elektron.platform

/**
 * Abstracción de operaciones de almacenamiento y archivos del entorno de ejecución.
 */
interface PlatformStorage {
    fun saveLocal(json: String)
    fun loadLocal(): String?
    fun download(filename: String, content: String, mimeType: String)
    fun openFile(acceptedExtension: String = ".elektron", onLoaded: (String) -> Unit)
}
