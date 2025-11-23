package at.crowdware.innernet

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import at.crowdware.innernet.ui.AppRoot
import at.crowdware.innernet.ui.ThemeMode
import at.crowdware.innernet.ui.ThemeStorage
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlin.js.console

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = document.getElementById("root") ?: error("Root element with id 'root' not found")
    ComposeViewport(root) {
        AppRoot(
            themeStorage = WebThemeStorage,
            loadSml = { fetchVariantSml() }
        )
    }
}

private object WebThemeStorage : ThemeStorage {
    override fun load(): ThemeMode =
        runCatching { window.localStorage.getItem("theme") }
            .getOrNull()
            ?.let { stored -> ThemeMode.values().firstOrNull { it.name.equals(stored, ignoreCase = true) } }
            ?: ThemeMode.Dark

    override fun save(mode: ThemeMode) {
        runCatching { window.localStorage.setItem("theme", mode.name) }
            .onFailure { console.warn("Could not persist theme: ${it.message}") }
    }
}

private suspend fun fetchSml(path: String): String {
    val response = window.fetch(path).await()
    return response.text().await()
}

private suspend fun fetchVariantSml(): String {
    val orientation = if (window.innerWidth > window.innerHeight) "ls" else "pt"
    val lang = (window.navigator.language ?: "en").take(2).lowercase().let { if (it in setOf("de", "en")) it else "en" }
    val baseName = "Home.$orientation.$lang.sml"
    val fallback = "home.sml"
    val paths = listOf(baseName, fallback)
    var lastError: Throwable? = null
    for (p in paths) {
        try {
            val res = window.fetch(p).await()
            if (res.ok) return res.text().await()
        } catch (t: Throwable) {
            lastError = t
        }
    }
    console.warn("Falling back to empty UI; tried $paths, last error: ${lastError?.message}")
    return "Page { id: \"empty\" title: \"InnerNet\" }"
}
