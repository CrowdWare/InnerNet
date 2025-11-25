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
//import at.crowdware.innernet.render.RenderSml
//import at.crowdware.innernet.render.SmlNode
//import at.crowdware.innernet.render.parseSml
import kotlinx.coroutines.launch
import at.crowdware.sms.ScriptEngine
import at.crowdware.sml.PropertyValue
import at.crowdware.smlrender.RenderSml
import at.crowdware.smlrender.SmlNode
import at.crowdware.sml.SmlHandler
import at.crowdware.sml.SmlParseException
import at.crowdware.sml.SmlSaxParser
import at.crowdware.sml.Span


interface ThemeStorage {
    fun load(): ThemeMode
    fun save(mode: ThemeMode)
}

enum class ThemeMode { Light, Dark; fun toggle() = if (this == Light) Dark else Light }

@Composable
fun AppRoot(
    themeStorage: ThemeStorage,
    loadSml: suspend () -> LoadedSml,
    loadPage: suspend (String) -> LoadedSml,
    loadScript: suspend (String) -> String?,
    loadStrings: suspend (String) -> String?,
    openWeb: suspend (String) -> Unit,
    language: String? = null,
    availableLanguages: List<String> = listOf("en", "de"),
    defaultLanguage: String = "en",
    onTitleChange: (String) -> Unit = {}
) {
    var themeMode by remember { mutableStateOf(themeStorage.load()) }
    var smlText by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var scriptEngine by remember { mutableStateOf<ScriptEngine?>(null) }
    var loadedName by remember { mutableStateOf<String?>(null) }
    var strings by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(themeMode) { themeStorage.save(themeMode) }

    LaunchedEffect(Unit) {
        loadUiAndScripts(
            load = loadSml,
            loadScript = loadScript,
            loadStrings = loadStrings,
            language = language,
            availableLanguages = availableLanguages,
            defaultLanguage = defaultLanguage,
            onSuccess = { text, engine ->
                smlText = text.text
                loadedName = text.name
                scriptEngine = engine
                strings = text.strings
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
                        scriptEngine = scriptEngine,
                        strings = strings,
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
                                    val msg = t.message ?: t.toString()
                                    error = msg
                                    updateStatusMessage(engine, msg)
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
                                                loadStrings = loadStrings,
                                                language = language,
                                                availableLanguages = availableLanguages,
                                                defaultLanguage = defaultLanguage,
                                                onSuccess = { text, engine ->
                                                    smlText = text.text
                                                    loadedName = text.name
                                                    scriptEngine = engine
                                                    strings = text.strings
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
    load: suspend () -> LoadedSml,
    loadScript: suspend (String) -> String?,
    loadStrings: suspend (String) -> String?,
    language: String?,
    availableLanguages: List<String>,
    defaultLanguage: String,
    onSuccess: (LoadedSml, ScriptEngine?) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val loaded = load()
        val strings = loadLocalizedStrings(loadStrings, language, availableLanguages, defaultLanguage).getOrDefault(emptyMap())
        var engine: ScriptEngine? = null
        var scriptError: Throwable? = null
        try {
            engine = prepareScriptEngine(loaded, loadScript, language)
        } catch (t: Throwable) {
            scriptError = t
            updateStatusMessage(engine, t.message ?: t.toString())
        }
        onSuccess(loaded.copy(strings = strings), engine)
        if (scriptError != null) onError(scriptError.message ?: scriptError.toString())
    } catch (t: Throwable) {
        onError(t.message ?: t.toString())
    }
}

private suspend fun prepareScriptEngine(
    loaded: LoadedSml,
    loadScript: suspend (String) -> String?,
    language: String?
): ScriptEngine {
    val engine = ScriptEngine.withStandardLibrary()
    engine.registerKotlinFunction("println") { args ->
        val msg = args.firstOrNull()?.toString() ?: "null"
        println(msg)
        null
    }
    loadScript("global.sms")?.let { engine.execute(it) }
    val pageScript = extractPageScriptName(loaded.text) ?: defaultScriptNameFor(loaded.name)
    if (pageScript != null) {
        loadScript(pageScript)?.let { engine.execute(it) }
    }
    language?.let { lang ->
        val escaped = lang.replace("\\", "\\\\").replace("\"", "\\\"")
        runCatching { engine.execute("""language = "$escaped"""") }
    }
    return engine
}

private fun extractPageScriptName(text: String): String? {
    val roots: List<SmlNode> = runCatching { parseSml(text) }.getOrNull() ?: return null
    val page = roots.firstOrNull { it.name == "Page" } ?: return null
    val prop = page.properties["script"] as? PropertyValue.StringValue
    return prop?.value
}

data class LoadedSml(val text: String, val name: String, val strings: Map<String, String> = emptyMap())
private data class LoadedStrings(val map: Map<String, String>)

private fun defaultScriptNameFor(name: String): String = "home.sms"

internal fun updateStatusMessage(engine: ScriptEngine?, message: String) {
    if (engine == null) return
    val escaped = message.replace("\\", "\\\\").replace("\"", "\\\"")
    runCatching { engine.execute("""statusMessage = "$escaped"""") }
}

private suspend fun loadLocalizedStrings(
    loadStrings: suspend (String) -> String?,
    language: String?,
    availableLanguages: List<String>,
    defaultLanguage: String
): Result<Map<String, String>> {
    val normalizedLang = language?.lowercase()
        ?.takeIf { availableLanguages.isEmpty() || availableLanguages.map { it.lowercase() }.contains(it) }
    val defaultLang = defaultLanguage.lowercase()

    val candidateLangs = buildList {
        if (normalizedLang != null) add(normalizedLang)
        if (defaultLang != normalizedLang) add(defaultLang)
    }

    var lastErr: Throwable? = null
    var defaultMap: Map<String, String> = emptyMap()
    var localizedMap: Map<String, String> = emptyMap()

    for (lang in candidateLangs) {
        val candidateFile = "strings_${lang}.sml"
        try {
            val txt = loadStrings(candidateFile)
            if (txt != null) {
                val parsed = parseStringsSml(txt, lang)
                if (lang == defaultLang) defaultMap = parsed else localizedMap = parsed
            }
        } catch (t: Throwable) {
            lastErr = t
        }
    }

    val merged = defaultMap + localizedMap
    return if (merged.isNotEmpty()) Result.success(merged)
    else Result.failure(lastErr ?: IllegalStateException("No strings file found"))
}

private fun parseStringsSml(text: String, language: String): Map<String, String> {
    val roots = runCatching { parseSml(text) }.getOrNull() ?: return emptyMap()
    val stringsNode = roots.firstOrNull { it.name == "Strings" } ?: return emptyMap()
    val langProp = (stringsNode.properties["lang"] as? PropertyValue.StringValue)?.value
    if (langProp != null && langProp.lowercase() != language.lowercase()) return emptyMap()
    val result = mutableMapOf<String, String>()
    stringsNode.children.filter { it.name == "String" }.forEach { strNode ->
        val key = (strNode.properties["key"] as? PropertyValue.StringValue)?.value
        val value = (strNode.properties["text"] as? PropertyValue.StringValue)?.value
        if (key != null && value != null) result[key] = value
    }
    return result
}

private class NodeBuildingHandler : SmlHandler {
    private val stack = ArrayDeque<MutableNode>()
    private val roots = mutableListOf<MutableNode>()

    override fun startElement(name: String) {
        stack.addLast(MutableNode(name))
    }

    override fun onProperty(name: String, value: PropertyValue) {
        val cur = stack.lastOrNull() ?: throw SmlParseException("Property outside of element", Span(0, 0, 0))
        cur.props[name] = value
    }

    override fun endElement(name: String) {
        val node = stack.removeLastOrNull() ?: throw SmlParseException("Mismatched end '$name'", Span(0, 0, 0))
        if (node.name != name) throw SmlParseException("Expected end of ${node.name}, got $name", Span(0, 0, 0))
        if (stack.isEmpty()) roots += node else stack.last().children += node
    }

    fun result(): List<SmlNode> = roots.map { it.toImmutable() }

    private data class MutableNode(
        val name: String,
        val props: MutableMap<String, PropertyValue> = LinkedHashMap(),
        val children: MutableList<MutableNode> = mutableListOf()
    ) {
        fun toImmutable(): SmlNode = SmlNode(name, props.toMap(), children.map { it.toImmutable() })
    }
}

internal fun parseSml(text: String): List<SmlNode> {
    val handler = NodeBuildingHandler()
    SmlSaxParser(text).parse(handler)
    return handler.result()
}