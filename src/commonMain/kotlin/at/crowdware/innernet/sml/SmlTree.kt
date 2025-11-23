package at.crowdware.innernet.sml

import at.crowdware.sml.PropertyValue
import at.crowdware.sml.SmlHandler
import at.crowdware.sml.SmlSaxParser

data class SmlNode(
    val name: String,
    val properties: MutableMap<String, MutableList<PropertyValue>> = mutableMapOf(),
    val children: MutableList<SmlNode> = mutableListOf()
)

object SmlTreeParser {
    fun parse(text: String): List<SmlNode> {
        val roots = mutableListOf<SmlNode>()
        val stack = ArrayDeque<SmlNode>()

        SmlSaxParser(text).parse(object : SmlHandler {
            override fun startElement(name: String) {
                stack.addLast(SmlNode(name))
            }

            override fun onProperty(name: String, value: PropertyValue) {
                val current = stack.last()
                current.properties.getOrPut(name) { mutableListOf() }.add(value)
            }

            override fun endElement(name: String) {
                val finished = stack.removeLast()
                if (stack.isEmpty()) roots += finished else stack.last().children += finished
            }
        })

        return roots
    }
}

fun SmlNode.string(name: String): String? = (properties[name]?.lastOrNull() as? PropertyValue.StringValue)?.value
fun SmlNode.int(name: String): Int? = (properties[name]?.lastOrNull() as? PropertyValue.IntValue)?.value
fun SmlNode.boolean(name: String): Boolean? = (properties[name]?.lastOrNull() as? PropertyValue.BooleanValue)?.value
fun SmlNode.stringList(name: String): List<String> = properties[name]?.mapNotNull { (it as? PropertyValue.StringValue)?.value } ?: emptyList()
