package at.crowdware.innernet

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import at.crowdware.innernet.render.RenderSml
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = document.getElementById("root") ?: error("Root element with id 'root' not found")
    ComposeViewport(root) {
        MaterialTheme {
            Surface(modifier = Modifier) {
                AppContent()
            }
        }
    }
}

@Composable
private fun AppContent() {
    var smlText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = window.fetch("home.sml").await()
            smlText = response.text().await()
        } catch (t: Throwable) {
            error = t.message ?: t.toString()
        }
    }

    when {
        error != null -> Text("Error: $error")
        smlText == null -> Text("Loading…")
        else -> RenderSml(smlText!!)
    }
}
