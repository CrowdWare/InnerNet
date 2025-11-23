@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package at.crowdware.innernet.render

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import at.crowdware.sml.PropertyValue
import at.crowdware.sml.SmlHandler
import at.crowdware.sml.SmlParseException
import at.crowdware.sml.SmlSaxParser
import at.crowdware.sml.Span
import kotlin.js.console

data class SmlNode(
    val name: String,
    val properties: Map<String, PropertyValue>,
    val children: List<SmlNode>
)

// ---------- Parsing ----------

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

private fun parseSml(text: String): List<SmlNode> {
    val handler = NodeBuildingHandler()
    SmlSaxParser(text).parse(handler)
    return handler.result()
}

// ---------- Helpers ----------

private fun Map<String, PropertyValue>.string(key: String): String? =
    (this[key] as? PropertyValue.StringValue)?.value

private fun Map<String, PropertyValue>.int(key: String): Int? =
    (this[key] as? PropertyValue.IntValue)?.value

private fun Map<String, PropertyValue>.float(key: String): Float? =
    (this[key] as? PropertyValue.FloatValue)?.value

private fun paddingValues(props: Map<String, PropertyValue>, defaultAll: Int): androidx.compose.foundation.layout.PaddingValues {
    props.int("padding")?.let { return androidx.compose.foundation.layout.PaddingValues(it.dp) }
    val raw = props.string("padding")?.trim() ?: return androidx.compose.foundation.layout.PaddingValues(defaultAll.dp)
    val parts = raw.split(Regex("\\s+")).filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
    return when (parts.size) {
        1 -> androidx.compose.foundation.layout.PaddingValues(parts[0].dp)
        2 -> androidx.compose.foundation.layout.PaddingValues(horizontal = parts[1].dp, vertical = parts[0].dp)
        4 -> androidx.compose.foundation.layout.PaddingValues(
            start = parts[0].dp,
            top = parts[1].dp,
            end = parts[2].dp,
            bottom = parts[3].dp
        )
        else -> androidx.compose.foundation.layout.PaddingValues(defaultAll.dp)
    }
}

// ---------- Rendering entry ----------

@Composable
fun RenderSml(text: String) {
    val roots = remember(text) { parseSml(text) }
    val page = roots.firstOrNull { it.name == "Page" }
    if (page != null) {
        renderNode(page)
    } else {
        roots.forEach { renderNode(it) }
    }
}

// ---------- Neutral render (no parent scope) ----------

@Composable
private fun renderNode(node: SmlNode) {
    when (node.name) {
        "Page" -> {
            val pad = paddingValues(node.properties, defaultAll = 16)
            println("page $pad")
            Column(
                modifier = Modifier.fillMaxSize().padding(pad),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                node.children.forEach { child -> renderNodeInColumn(child) }
            }
        }
        "Column" -> {
            val spacing = node.properties.int("spacing") ?: 0
            val pad = paddingValues(node.properties, defaultAll = 0)
            val align = when (node.properties.string("alignment")) {
                "center" -> Alignment.CenterHorizontally
                else -> Alignment.Start
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(pad),
                verticalArrangement = Arrangement.spacedBy(spacing.dp),
                horizontalAlignment = align
            ) {
                node.children.forEach { child -> renderNodeInColumn(child) }
            }
        }
        "Row" -> {
            val spacing = node.properties.int("spacing") ?: 0
            val pad = paddingValues(node.properties, defaultAll = 0)
            Row(
                modifier = Modifier.fillMaxWidth().padding(pad),
                horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                node.children.forEach { child -> renderNodeInRow(child) }
            }
        }
        "Grid" -> {
            val spacing = node.properties.int("spacing") ?: 8
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.dp)
            ) {
                node.children.forEach { child -> renderNodeInColumn(child) }
            }
        }
        "Spacer" -> Spacer(Modifier.height((node.properties.int("amount") ?: 0).dp))
        "Text", "Label" -> Text(node.properties.string("text") ?: "")
        "Link" -> {
            val label = node.properties.string("text").orEmpty()
            val href = node.properties.string("href").orEmpty()
            ClickableText(
                text = AnnotatedString(label),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) { console.log("open url: $href") }
        }
        "Markdown" -> {
            val raw = node.properties.string("text") ?: ""
            val (heading, body) = stripHeading(raw)
            val annotated: AnnotatedString = parseInlineMarkdown(body)
            val style = when (heading) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                else -> MaterialTheme.typography.bodyMedium
            }.copy(color = MaterialTheme.colorScheme.onBackground)
            ClickableText(text = annotated, style = style) { offset ->
                annotated.getStringAnnotations("URL", offset, offset).firstOrNull()
                    ?.let { console.log("open url: ${it.item}") }
            }
        }
        "Button" -> {
            val txt = node.properties.string("text")
            Button(onClick = { console.log("action: ${node.properties.string("action")}") }) {
                if (txt != null) Text(txt)
                node.children.forEach { child -> renderNode(child) }
            }
        }
        else -> {
            node.children.forEach { child -> renderNode(child) }
        }
    }
}

// ---------- Column scope render ----------

@Composable
private fun ColumnScope.renderNodeInColumn(node: SmlNode) {
    when (node.name) {
        "Column" -> {
            // Nested Column stays in ColumnScope for children.
            val spacing = node.properties.int("spacing") ?: 0
            val pad = paddingValues(node.properties, defaultAll = if (node.name == "Page") 16 else 0)
            val align = when (node.properties.string("alignment")) {
                "center" -> Alignment.CenterHorizontally
                else -> Alignment.Start
            }
            Column(
                modifier = Modifier
                    .then(if (node.name == "Page") Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .padding(pad),
                verticalArrangement = Arrangement.spacedBy(spacing.dp),
                horizontalAlignment = align
            ) {
                node.children.forEach { child -> renderNodeInColumn(child) }
            }
        }
        "Row" -> {
            val spacing = node.properties.int("spacing") ?: 0
            val pad = paddingValues(node.properties, defaultAll = 0)
            Row(
                modifier = Modifier.fillMaxWidth().padding(pad),
                horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                node.children.forEach { child -> renderNodeInRow(child) }
            }
        }
        "Grid" -> {
            val spacing = node.properties.int("spacing") ?: 8
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.dp)
            ) {
                node.children.forEach { child -> renderNodeInColumn(child) }
            }
        }
        "Spacer" -> {
            val amount = node.properties.int("amount") ?: 0
            val w = node.properties.int("weight")?.toFloat()
            val mod = if (w != null) Modifier.weight(weight = w) else Modifier.height(amount.dp)
            Spacer(mod)
        }
        "Text", "Label" -> Text(node.properties.string("text") ?: "")
        "Link" -> {
            val label = node.properties.string("text").orEmpty()
            val href = node.properties.string("href").orEmpty()
            ClickableText(
                text = AnnotatedString(label),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) { console.log("open url: $href") }
        }
        "Markdown" -> {
            val raw = node.properties.string("text") ?: ""
            val (heading, body) = stripHeading(raw)
            val annotated: AnnotatedString = parseInlineMarkdown(body)
            val style = when (heading) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                else -> MaterialTheme.typography.bodyMedium
            }.copy(color = MaterialTheme.colorScheme.onBackground)
            ClickableText(text = annotated, style = style) { offset ->
                annotated.getStringAnnotations("URL", offset, offset).firstOrNull()
                    ?.let { console.log("open url: ${it.item}") }
            }
        }
        "Button" -> {
            val txt = node.properties.string("text")
            Button(onClick = { console.log("action: ${node.properties.string("action")}") }) {
                if (txt != null) Text(txt)
                node.children.forEach { child -> renderNode(child) }
            }
        }
        else -> {
            node.children.forEach { child -> renderNodeInColumn(child) }
        }
    }
}

// ---------- Row scope render ----------

@Composable
private fun RowScope.renderNodeInRow(node: SmlNode) {
    when (node.name) {
        "Row" -> {
            val spacing = node.properties.int("spacing") ?: 0
            val pad = paddingValues(node.properties, defaultAll = 0)
            Row(
                modifier = Modifier.fillMaxWidth().padding(pad),
                horizontalArrangement = Arrangement.spacedBy(spacing.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                node.children.forEach { child -> renderNodeInRow(child) }
            }
        }
        "Column" -> {
            val spacing = node.properties.int("spacing") ?: 0
            val pad = paddingValues(node.properties, defaultAll = if (node.name == "Page") 16 else 0)
            val align = when (node.properties.string("alignment")) {
                "center" -> Alignment.CenterHorizontally
                else -> Alignment.Start
            }
            Column(
                modifier = Modifier
                    .then(if (node.name == "Page") Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .padding(pad),
                verticalArrangement = Arrangement.spacedBy(spacing.dp),
                horizontalAlignment = align
            ) {
                node.children.forEach { child -> renderNodeInColumn(child) }
            }
        }
        "Grid" -> {
            val spacing = node.properties.int("spacing") ?: 8
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.dp)
            ) {
                node.children.forEach { child -> renderNodeInColumn(child) }
            }
        }
        "Spacer" -> {
            val amount = node.properties.int("amount") ?: 0
            val w = node.properties.int("weight")?.toFloat()
            val mod = if (w != null) Modifier.weight(weight = w) else Modifier.width(amount.dp)
            Spacer(mod)
        }
        "Text", "Label" -> Text(node.properties.string("text") ?: "")
        "Link" -> {
            val label = node.properties.string("text").orEmpty()
            val href = node.properties.string("href").orEmpty()
            ClickableText(
                text = AnnotatedString(label),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) { console.log("open url: $href") }
        }
        "Markdown" -> {
            val raw = node.properties.string("text") ?: ""
            val (heading, body) = stripHeading(raw)
            val annotated: AnnotatedString = parseInlineMarkdown(body)
            val style = when (heading) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                else -> MaterialTheme.typography.bodyMedium
            }.copy(color = MaterialTheme.colorScheme.onBackground)
            ClickableText(text = annotated, style = style) { offset ->
                annotated.getStringAnnotations("URL", offset, offset).firstOrNull()
                    ?.let { console.log("open url: ${it.item}") }
            }
        }
        "Button" -> {
            val txt = node.properties.string("text")
            Button(onClick = { console.log("action: ${node.properties.string("action")}") }) {
                if (txt != null) Text(txt)
                node.children.forEach { child -> renderNode(child) }
            }
        }
        else -> {
            node.children.forEach { child -> renderNodeInRow(child) }
        }
    }
}

// ---------- Box scope render ----------

@Composable
fun BoxScope.RenderNode(node: SmlNode) {
    renderNode(node)
}
