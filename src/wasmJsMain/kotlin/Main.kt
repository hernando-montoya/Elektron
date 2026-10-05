import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import org.nicokosi.elektron.app.App
import org.nicokosi.elektron.platform.LocalPlatformStorage
import org.nicokosi.elektron.platform.WasmBrowserStorage

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = document.getElementById("ComposeTarget")!!
    ComposeViewport(root) {
        CompositionLocalProvider(LocalPlatformStorage provides WasmBrowserStorage) {
            App()
        }
    }
}
