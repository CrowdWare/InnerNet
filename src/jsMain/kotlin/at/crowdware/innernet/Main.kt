package at.crowdware.innernet

import androidx.compose.material3.Text
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document
import at.crowdware.sml.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val text = """
        /*
        Comment
        */
        Page {
            id: "main"
            title: "Hello World"
            visible: true
            width: 800
            height: 600

            Row {
                spacing: 10
                Label { text: "Hello" }
                Label { text: "SML" }
            }
        }
    """.trimIndent()

    ComposeViewport(document.getElementById("root")!!) {
        RenderSml(text)
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
        Column(Modifier.fillMaxSize().padding(pad.dp)) { content() }
    }
    // Row
    register("Row") { props, content ->
        val spacing = props.int("spacing") ?: 0
        Row(Modifier.fillMaxWidth()) {
            content()
            if (spacing > 0) Spacer(Modifier.width(spacing.dp))
        }
    }
    // Column
    register("Column") { props, content ->
        val spacing = props.int("spacing") ?: 0
        Column(Modifier.fillMaxWidth()) {
            content()
            if (spacing > 0) Spacer(Modifier.height(spacing.dp))
        }
    }
    // Label -> Text
    register("Label") { props, _ ->
        Text(props.string("text") ?: "")
    }
    // Button
    register("Button") { props, content ->
        val txt = props.string("text")
        Button(onClick = { /* TODO: Aktion */ }) {
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