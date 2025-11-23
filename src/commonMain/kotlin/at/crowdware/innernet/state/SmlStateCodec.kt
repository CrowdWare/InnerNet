package at.crowdware.innernet.state

import at.crowdware.sml.PropertyValue
import at.crowdware.sml.SmlHandler
import at.crowdware.sml.SmlSaxParser

/**
 * Streaming encoder/decoder for InnerNetState using the SAX-style SML parser.
 * No DOM tree is built; we stitch domain objects together as events arrive.
 */
object SmlStateCodec {

    fun encode(state: InnerNetState): String = buildString {
        appendLine("InnerNetState {")
        appendLine("  version: ${state.version}")
        state.profile?.let { profile ->
            appendLine("  Profile {")
            appendLine("    nickname: \"${profile.nickname}\"")
            appendLine("  }")
        }
        state.avatars.forEach { avatar ->
            appendLine("  Avatar {")
            appendLine("    id: \"${avatar.id}\"")
            appendLine("    level: ${avatar.level}")
            appendLine("    xp: ${avatar.xp}")
            avatar.visual?.let { v ->
                appendLine("    Visual {")
                v.gender?.let { appendLine("      gender: \"$it\"") }
                v.hairColor?.let { appendLine("      hairColor: \"$it\"") }
                v.hairLength?.let { appendLine("      hairLength: \"$it\"") }
                v.eyeColor?.let { appendLine("      eyeColor: \"$it\"") }
                v.skinTone?.let { appendLine("      skinTone: \"$it\"") }
                v.style?.let { appendLine("      style: \"$it\"") }
                appendLine("    }")
            }
            appendLine("  }")
        }
        state.today?.let { today ->
            appendLine("  Today {")
            appendLine("    date: \"${today.date}\"")
            today.selectedAvatarId?.let { appendLine("    selectedAvatarId: \"$it\"") }
            today.quests.forEach { quest ->
                appendLine("    Quest {")
                appendLine("      id: \"${quest.id}\"")
                appendLine("      title: \"${quest.title}\"")
                appendLine("      xp: ${quest.xp}")
                appendLine("      done: ${quest.done}")
                appendLine("    }")
            }
            appendLine("  }")
        }
        state.answers.forEach { ans ->
            appendLine("  Answer {")
            appendLine("    date: \"${ans.date}\"")
            appendLine("    question: \"${ans.question}\"")
            appendLine("    text: \"${ans.text}\"")
            appendLine("  }")
        }
        appendLine("}")
    }

    fun decode(text: String): InnerNetState {
        val handler = StateBuildingHandler()
        SmlSaxParser(text).parse(handler)
        return handler.build()
    }
}

// -- Streaming builder -----------------------------------------------------------------------------

private class StateBuildingHandler : SmlHandler {
    private var version: Int = 1
    private var profile: Profile? = null
    private val avatars = mutableListOf<Avatar>()
    private var today: Element.TodayDraft? = null
    private val answers = mutableListOf<Answer>()

    private val stack = ArrayDeque<Element>()

    override fun startElement(name: String) {
        when (name) {
            "InnerNetState" -> stack.addLast(Element.Root)
            "Profile" -> stack.addLast(Element.ProfileDraft())
            "Avatar" -> stack.addLast(Element.AvatarDraft())
            "Visual" -> stack.addLast(Element.VisualDraft())
            "Today" -> stack.addLast(Element.TodayDraft())
            "Quest" -> stack.addLast(Element.QuestDraft())
            "Answer" -> stack.addLast(Element.AnswerDraft())
            else -> stack.addLast(Element.Unknown(name))
        }
    }

    override fun onProperty(name: String, value: PropertyValue) {
        val key = name.lowercase()
        val current = stack.lastOrNull() ?: return
        when (current) {
            is Element.ProfileDraft -> current.nickname = value.asString()
            is Element.AvatarDraft -> when (key) {
                "id" -> current.id = value.asString()
                "level" -> current.level = value.asInt()
                "xp" -> current.xp = value.asInt()
            }
            is Element.VisualDraft -> when (key) {
                "gender" -> current.gender = value.asString()
                "haircolor" -> current.hairColor = value.asString()
                "hairlength" -> current.hairLength = value.asString()
                "eyecolor" -> current.eyeColor = value.asString()
                "skintone" -> current.skinTone = value.asString()
                "style" -> current.style = value.asString()
            }
            is Element.TodayDraft -> when (key) {
                "date" -> current.date = value.asString()
                "selectedavatarid" -> current.selectedAvatarId = value.asString()
            }
            is Element.QuestDraft -> when (key) {
                "id" -> current.id = value.asString()
                "title" -> current.title = value.asString()
                "xp" -> current.xp = value.asInt()
                "done" -> current.done = value.asBoolean()
            }
            is Element.AnswerDraft -> when (key) {
                "date" -> current.date = value.asString()
                "question" -> current.question = value.asString()
                "text" -> current.text = value.asString()
            }
            is Element.Root -> if (key == "version") version = value.asInt() ?: version
            else -> {} // ignore unknowns
        }
    }

    override fun endElement(name: String) {
        val finished = stack.removeLastOrNull() ?: return
        val parent = stack.lastOrNull()
        when (finished) {
            is Element.ProfileDraft -> profile = Profile(finished.nickname.orEmpty())
            is Element.VisualDraft -> when (parent) {
                is Element.AvatarDraft -> parent.visual = finished.toVisual()
                else -> {}
            }
            is Element.AvatarDraft -> avatars += finished.toAvatar()
            is Element.QuestDraft -> when (parent) {
                is Element.TodayDraft -> parent.quests += finished.toQuest()
                else -> {}
            }
            is Element.TodayDraft -> today = finished
            is Element.AnswerDraft -> answers += finished.toAnswer()
            else -> {}
        }
    }

    fun build(): InnerNetState = InnerNetState(
        version = version,
        profile = profile,
        avatars = avatars.toList(),
        today = today?.toToday(),
        answers = answers.toList()
    )
}

// -- Element drafts --------------------------------------------------------------------------------

private sealed interface Element {
    data object Root : Element
    class ProfileDraft(var nickname: String? = null) : Element
    class AvatarDraft(
        var id: String? = null,
        var level: Int? = null,
        var xp: Int? = null,
        var visual: Visual? = null
    ) : Element {
        fun toAvatar() = Avatar(
            id = id.orEmpty(),
            level = level ?: 1,
            xp = xp ?: 0,
            visual = visual
        )
    }
    class VisualDraft(
        var gender: String? = null,
        var hairColor: String? = null,
        var hairLength: String? = null,
        var eyeColor: String? = null,
        var skinTone: String? = null,
        var style: String? = null
    ) : Element {
        fun toVisual() = Visual(gender, hairColor, hairLength, eyeColor, skinTone, style)
    }
    class TodayDraft(
        var date: String? = null,
        var selectedAvatarId: String? = null,
        val quests: MutableList<Quest> = mutableListOf()
    ) : Element {
        fun toToday() = Today(
            date = date.orEmpty(),
            selectedAvatarId = selectedAvatarId,
            quests = quests.toList()
        )
    }
    class QuestDraft(
        var id: String? = null,
        var title: String? = null,
        var xp: Int? = null,
        var done: Boolean? = null
    ) : Element {
        fun toQuest() = Quest(
            id = id.orEmpty(),
            title = title.orEmpty(),
            xp = xp ?: 0,
            done = done ?: false
        )
    }
    class AnswerDraft(
        var date: String? = null,
        var question: String? = null,
        var text: String? = null
    ) : Element {
        fun toAnswer() = Answer(
            date = date.orEmpty(),
            question = question.orEmpty(),
            text = text.orEmpty()
        )
    }
    class Unknown(val name: String) : Element
}

// -- Property helpers -------------------------------------------------------------------------------

private fun PropertyValue.asString(): String? = (this as? PropertyValue.StringValue)?.value
private fun PropertyValue.asInt(): Int? = (this as? PropertyValue.IntValue)?.value
private fun PropertyValue.asBoolean(): Boolean? = (this as? PropertyValue.BooleanValue)?.value
