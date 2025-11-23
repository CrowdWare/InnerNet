package at.crowdware.innernet.ui

/*
import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.css.StyleScope
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text

@Composable
fun RenderNode(node: UiNode) {
    when (node) {
        is Page -> container("page-container") { node.children.forEach { RenderNode(it) } }
        is Column -> container("column", node.spacing, node.padding, node.alignment) { node.children.forEach { RenderNode(it) } }
        is Grid -> container("grid", node.spacing, null, null) { node.children.forEach { RenderNode(it) } }
        is Text -> when (node.style) {
            "title" -> H1 { Text(node.text.orEmpty()) }
            "subtitle" -> H2 { Text(node.text.orEmpty()) }
            "question" -> P({ classes("question") }) { Text(node.text.orEmpty()) }
            else -> P { Text(node.text.orEmpty()) }
        }
        is Button -> Button(attrs = { classes("btn"); onClick { console.log("action: ${node.action}") } }) { Text(node.text.orEmpty()) }
        is Spacer -> Div({ classes("spacer") }) {}
        is Dropdown -> Div({ classes("dropdown") }) {
            P { Text(node.label.orEmpty()) }
            node.options.forEach { opt -> P({ classes("option") }) { Text(opt) } }
        }
        is AvatarCard -> Div({ classes("card") }) {
            H2 { Text(node.title.orEmpty()) }
            P { Text(node.subtitle.orEmpty()) }
        }
    }
}

@Composable
private fun container(cls: String, spacing: Int? = null, padding: Int? = null, alignment: String? = null, content: @Composable () -> Unit) {
    val align = when (alignment) {
        "center" -> "center"
        else -> "flex-start"
    }
    Div({
        classes(cls)
        if (spacing != null) style { property("gap", "${spacing}px") }
        if (padding != null) style { property("padding", "${padding}px") }
        style {
            property("align-items", align)
        }
    }) { content() }
}
*/