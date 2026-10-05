package org.nicokosi.elektron.platform

import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader

external fun encodeURIComponent(uri: String): String

/**
 * Implementación WasmJS/Web de PlatformStorage.
 */
object WasmBrowserStorage : PlatformStorage {

    private const val STORAGE_KEY = "elektron_cad_autosave_project"

    override fun saveLocal(json: String) {
        try {
            window.localStorage.setItem(STORAGE_KEY, json)
        } catch (e: Exception) {
            println("Erreur sauvegarde locale: ${e.message}")
        }
    }

    override fun loadLocal(): String? {
        return try {
            window.localStorage.getItem(STORAGE_KEY)
        } catch (e: Exception) {
            null
        }
    }

    override fun download(filename: String, content: String, mimeType: String) {
        try {
            val encodedUri = "data:$mimeType;charset=utf-8," + encodeURIComponent(content)
            val link = document.createElement("a") as HTMLAnchorElement
            link.href = encodedUri
            link.download = filename
            document.body?.appendChild(link)
            link.click()
            document.body?.removeChild(link)
        } catch (e: Exception) {
            println("Erreur téléchargement: ${e.message}")
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun openFile(acceptedExtension: String, onLoaded: (String) -> Unit) {
        try {
            val input = document.createElement("input") as HTMLInputElement
            input.type = "file"
            input.accept = acceptedExtension
            input.style.display = "none"

            input.onchange = {
                val file = input.files?.item(0)
                if (file != null) {
                    val reader = FileReader()
                    reader.onload = {
                        val text = reader.result?.toString() ?: ""
                        if (text.isNotEmpty()) {
                            onLoaded(text)
                        }
                    }
                    reader.readAsText(file)
                }
            }

            document.body?.appendChild(input)
            input.click()
            document.body?.removeChild(input)
        } catch (e: Exception) {
            println("Erreur ouverture fichier: ${e.message}")
        }
    }
}
