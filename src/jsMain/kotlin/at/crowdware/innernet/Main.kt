package at.crowdware.innernet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeViewport
import at.crowdware.sml.PropertyValue
import at.crowdware.sml.SmlHandler
import at.crowdware.sml.SmlParseException
import at.crowdware.sml.SmlSaxParser
import at.crowdware.sml.Span
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val root = document.getElementById("root") ?: error("Root element with id 'root' not found")
    ComposeViewport(root) {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
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
        }
    }
}


// --- Registry für die Zuordnung von SML-Tags zu Compose-Komponenten -----------------------------

typealias SmlRenderer = @Composable (props: Map<String, PropertyValue>, content: @Composable () -> Unit) -> Unit

class SmlRenderRegistry {
    private val map = mutableMapOf<String, SmlRenderer>()
    fun register(name: String, renderer: SmlRenderer) { map[name] = renderer }
    fun resolve(name: String): SmlRenderer? = map[name]
}

fun defaultSmlRegistry(): SmlRenderRegistry = SmlRenderRegistry().apply {
    // Page -> Column mit Padding
    register("Page") { props, content ->
        val pad = props.int("padding") ?: 16
        Column(
            modifier = Modifier.fillMaxSize().padding(pad.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) { content() }
    }
    // Row
    register("Row") { props, content ->
        val spacing = props.int("spacing") ?: 0
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.dp),
            verticalAlignment = Alignment.CenterVertically
        ) { content() }
    }
    // Column
    register("Column") { props, content ->
        val spacing = props.int("spacing") ?: 0
        val pad = props.int("padding") ?: 0
        val align = when (props.string("alignment")) {
            "center" -> Alignment.CenterHorizontally
            else -> Alignment.Start
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(pad.dp),
            verticalArrangement = Arrangement.spacedBy(spacing.dp),
            horizontalAlignment = align
        ) { content() }
    }
    // Label/Text -> Text
    val textRenderer: SmlRenderer = { props, _ -> Text(props.string("text") ?: "") }
    register("Label", textRenderer)
    register("Text", textRenderer)
    // Grid (simple fallback to Column spacing)
    register("Grid") { props, content ->
        val spacing = props.int("spacing") ?: 8
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.dp)
        ) { content() }
    }
    // Spacer
    register("Spacer") { props, _ ->
        val amount = props.int("amount") ?: 0
        Spacer(Modifier.height(amount.dp))
    }
    // Button
    register("Button") { props, content ->
        val txt = props.string("text")
        Button(onClick = { console.log("action: ${props.string("action")}") }) {
            if (txt != null) Text(txt)
            content()
        }
    }
}

// --- Hilfsfunktionen für PropertyValues ----------------------------------------------------------

private fun Map<String, PropertyValue>.string(key: String): String? =
    (this[key] as? PropertyValue.StringValue)?.value
private fun Map<String, PropertyValue>.int(key: String): Int? =
    (this[key] as? PropertyValue.IntValue)?.value
private fun Map<String, PropertyValue>.bool(key: String): Boolean? =
    (this[key] as? PropertyValue.BooleanValue)?.value

// --- SAX ➜ Compose Builder ----------------------------------------------------------------------

private class ComposeBuildingHandler(
    private val registry: SmlRenderRegistry
) : SmlHandler {
    private data class Frame(
        val name: String,
        val props: MutableMap<String, PropertyValue> = LinkedHashMap(),
        val children: MutableList<@Composable () -> Unit> = mutableListOf()
    )
    private val stack = ArrayDeque<Frame>()
    private var root: (@Composable () -> Unit)? = null

    override fun startElement(name: String) {
        stack.addLast(Frame(name))
    }

    override fun onProperty(name: String, value: PropertyValue) {
        val cur = stack.lastOrNull() ?: throw SmlParseException("Property outside of element", Span(0,0,0))
        cur.props[name] = value
    }

    override fun endElement(name: String) {
        val f = stack.removeLastOrNull() ?: throw SmlParseException("Mismatched end '$name'", Span(0,0,0))
        if (f.name != name) throw SmlParseException("Expected end of ${f.name}, got $name", Span(0,0,0))
        val renderer = registry.resolve(name) ?: { _, content -> content() }
        val node: @Composable () -> Unit = { renderer(f.props) { f.children.forEach { it() } } }
        if (stack.isEmpty()) root = node else stack.last().children.add(node)
    }

    fun result(): (@Composable () -> Unit) = root ?: { }
}

private fun buildComposable(text: String, registry: SmlRenderRegistry): @Composable () -> Unit {
    val h = ComposeBuildingHandler(registry)
    SmlSaxParser(text).parse(h)
    return h.result()
}

// --- Öffentlicher Einstiegspunkt ----------------------------------------------------------------

@Composable
fun RenderSml(text: String, registry: SmlRenderRegistry = defaultSmlRegistry()) {
    val node = remember(text, registry) { mutableStateOf(buildComposable(text, registry)) }
    node.value()
}
