package at.crowdware.innernet.ui


import at.crowdware.innernet.sml.SmlNode
import at.crowdware.innernet.sml.SmlTreeParser
import at.crowdware.innernet.sml.int
import at.crowdware.innernet.sml.string
import at.crowdware.innernet.sml.stringList

object SmlUiParser {
    fun parse(text: String): List<UiNode> =
        SmlTreeParser.parse(text).mapNotNull { parseNode(it) }

    private fun parseNode(el: SmlNode): UiNode? = when (el.name) {
        "Page" -> Page(
            id = el.string("id"),
            title = el.string("title"),
            children = el.children.mapNotNull { parseNode(it) }
        )
        "Column" -> Column(
            id = el.string("id"),
            alignment = el.string("alignment"),
            spacing = el.int("spacing"),
            padding = el.int("padding"),
            children = el.children.mapNotNull { parseNode(it) }
        )
        "Grid" -> Grid(
            id = el.string("id"),
            columns = el.int("columns"),
            spacing = el.int("spacing"),
            children = el.children.mapNotNull { parseNode(it) }
        )
        "Text" -> Text(
            id = el.string("id"),
            text = el.string("text"),
            style = el.string("style")
        )
        "Button" -> Button(
            id = el.string("id"),
            text = el.string("text"),
            action = el.string("action")
        )
        "Spacer" -> Spacer(
            id = el.string("id"),
            amount = el.int("amount")
        )
        "Dropdown" -> Dropdown(
            id = el.string("id"),
            label = el.string("label"),
            options = el.stringList("option")
        )
        "AvatarCard" -> AvatarCard(
            id = el.string("id"),
            title = el.string("title"),
            subtitle = el.string("subtitle"),
            action = el.string("action")
        )
        // Unknown nodes are ignored for now.
        else -> null
    }
}
