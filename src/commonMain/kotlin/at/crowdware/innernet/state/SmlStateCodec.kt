package at.crowdware.innernet.state

import at.crowdware.innernet.sml.*

/*
object SmlStateCodec {
    fun decode(text: String): InnerNetState {
        val root = SmlTreeParser.parse(text).firstOrNull() ?: error("Empty SML state")
        require(root.name == "InnerNetState") { "Unexpected root ${root.name}" }

        val profile = root.children.firstOrNull { it.name == "Profile" }?.let {
            Profile(nickname = it.string("Nickname") ?: "")
        }
        val avatars = root.children.filter { it.name == "Avatar" }.map { decodeAvatar(it) }
        val today = root.children.firstOrNull { it.name == "Today" }?.let { decodeToday(it) }
        val version = root.int("Version") ?: 1
        return InnerNetState(version = version, profile = profile, avatars = avatars, today = today)
    }

    fun encode(state: InnerNetState): String = buildString {
        appendLine("InnerNetState {")
        appendLine("  Version: ${state.version}")
        state.profile?.let { profile ->
            appendLine("  Profile {")
            appendLine("    Nickname: \"${profile.nickname}\"")
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
        appendLine("}")
    }

    private fun decodeAvatar(el: SmlElement): Avatar {
        val visual = el.children.firstOrNull { it.name == "Visual" }?.let { v ->
            Visual(
                gender = v.string("gender"),
                hairColor = v.string("hairColor"),
                hairLength = v.string("hairLength"),
                eyeColor = v.string("eyeColor"),
                skinTone = v.string("skinTone"),
                style = v.string("style")
            )
        }
        return Avatar(
            id = el.string("id") ?: "",
            level = el.int("level") ?: 1,
            xp = el.int("xp") ?: 0,
            visual = visual
        )
    }

    private fun decodeToday(el: SmlElement): Today {
        val quests = el.children.filter { it.name == "Quest" }.map { q ->
            Quest(
                id = q.string("id") ?: "",
                title = q.string("title") ?: "",
                xp = q.int("xp") ?: 0,
                done = q.boolean("done") ?: false
            )
        }
        return Today(
            date = el.string("date") ?: "",
            selectedAvatarId = el.string("selectedAvatarId"),
            quests = quests
        )
    }
}
*/