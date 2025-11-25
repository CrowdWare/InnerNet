package at.crowdware.innernet.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.crowdware.innernet.render.RenderSml
import at.crowdware.innernet.render.SmlNode
import at.crowdware.innernet.render.parseSml
import kotlinx.coroutines.launch
import at.crowdware.sms.ScriptEngine
import at.crowdware.sml.PropertyValue

interface ThemeStorage {
    fun load(): ThemeMode
    fun save(mode: ThemeMode)
}

enum class ThemeMode { Light, Dark; fun toggle() = if (this == Light) Dark else Light }

@Composable
fun AppRoot(
    themeStorage: ThemeStorage,
    loadSml: suspend () -> String,
    loadPage: suspend (String) -> String,
    loadScript: suspend (String) -> String?,
    openWeb: suspend (String) -> Unit,
    onTitleChange: (String) -> Unit = {}
) {
    var themeMode by remember { mutableStateOf(themeStorage.load()) }
    var smlText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var scriptEngine by remember { mutableStateOf<ScriptEngine?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(themeMode) { themeStorage.save(themeMode) }

    LaunchedEffect(Unit) {
        loadUiAndScripts(
            load = loadSml,
            loadScript = loadScript,
            onSuccess = { text, engine ->
                smlText = text
                scriptEngine = engine
            },
            onError = { err -> error = err }
        )
    }

    val colors = when (themeMode) {
        ThemeMode.Dark -> darkColorScheme()
        ThemeMode.Light -> lightColorScheme()
    }

    MaterialTheme(colorScheme = colors) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { themeMode = themeMode.toggle() }) {
                        val target = if (themeMode == ThemeMode.Light) Icons.Filled.DarkMode else Icons.Filled.LightMode
                        val desc = if (themeMode == ThemeMode.Light) "Switch to dark mode" else "Switch to light mode"
                        Icon(imageVector = target, contentDescription = desc)
                    }
                }
                when {
                    error != null -> Text("Error: $error")
                    smlText == null -> Text("Loading…")
                    else -> RenderSml(
                        text = smlText!!,
                        onPageTitle = onTitleChange,
                        onScriptClick = { script ->
                            scope.launch {
                                val engine = scriptEngine
                                if (engine == null) {
                                    error = "Kein ScriptEngine geladen"
                                    return@launch
                                }
                                try {
                                    engine.execute(script)
                                } catch (t: Throwable) {
                                    error = t.message ?: t.toString()
                                }
                            }
                        },
                        onLinkClick = { link ->
                            scope.launch {
                                when {
                                    link.startsWith("page:") -> {
                                        val page = link.removePrefix("page:").trim()
                                        if (page.isEmpty()) return@launch
                                        smlText = null
                                        error = null
                                        try {
                                            loadUiAndScripts(
                                                load = { loadPage(page) },
                                                loadScript = loadScript,
                                                onSuccess = { text, engine ->
                                                    smlText = text
                                                    scriptEngine = engine
                                                },
                                                onError = { err -> error = err }
                                            )
                                        } catch (t: Throwable) {
                                            error = t.message ?: t.toString()
                                        }
                                    }
                                    link.startsWith("web:") -> {
                                        val target = link.removePrefix("web:").trim()
                                        if (target.isEmpty()) return@launch
                                        try {
                                            openWeb(target)
                                        } catch (t: Throwable) {
                                            error = t.message ?: t.toString()
                                        }
                                    }
                                    else -> error = "Unbekannter Link: $link"
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

private suspend fun loadUiAndScripts(
    load: suspend () -> String,
    loadScript: suspend (String) -> String?,
    onSuccess: (String, ScriptEngine?) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val text = load()
        var engine: ScriptEngine? = null
        var scriptError: Throwable? = null
        try {
            engine = prepareScriptEngine(text, loadScript)
        } catch (t: Throwable) {
            scriptError = t
        }
        onSuccess(text, engine)
        if (scriptError != null) onError(scriptError.message ?: scriptError.toString())
    } catch (t: Throwable) {
        onError(t.message ?: t.toString())
    }
}

private suspend fun prepareScriptEngine(
    smlText: String,
    loadScript: suspend (String) -> String?
): ScriptEngine {
    val engine = ScriptEngine.withStandardLibrary()
    engine.registerKotlinFunction("println") { args ->
        val msg = args.firstOrNull()?.toString() ?: "null"
        println(msg)
        null
    }
    loadScript("global.sms")?.let { engine.execute(it) }
    val pageScript = extractPageScriptName(smlText)
    if (pageScript != null) {
        loadScript(pageScript)?.let { engine.execute(it) }
    }
    return engine
}

private fun extractPageScriptName(text: String): String? {
    val roots: List<SmlNode> = runCatching { parseSml(text) }.getOrNull() ?: return null
    val page = roots.firstOrNull { it.name == "Page" } ?: return null
    val prop = page.properties["script"] as? PropertyValue.StringValue
    return prop?.value
}
