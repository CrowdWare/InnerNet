package at.crowdware.innernet
/* 
import at.crowdware.innernet.state.SmlStateCodec
import at.crowdware.innernet.ui.SmlUiParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmlParsingTest {
    @Test
    fun parsesHomePage() {
        val text = homeSml
        val nodes = SmlUiParser.parse(text)
        assertTrue(nodes.isNotEmpty())
    }

    @Test
    fun roundTripsState() {
        val state = sampleState()
        val encoded = SmlStateCodec.encode(state)
        val decoded = SmlStateCodec.decode(encoded)
        assertEquals(state.profile?.nickname, decoded.profile?.nickname)
        assertEquals(state.avatars.first().visual?.hairColor, decoded.avatars.first().visual?.hairColor)
    }

    private fun sampleState() = at.crowdware.innernet.state.InnerNetState(
        profile = at.crowdware.innernet.state.Profile("VisionSeeker"),
        avatars = listOf(
            at.crowdware.innernet.state.Avatar(
                id = "visionary",
                level = 2,
                xp = 22,
                visual = at.crowdware.innernet.state.Visual(
                    gender = "neutral",
                    hairColor = "brown",
                    hairLength = "long",
                    eyeColor = "green",
                    skinTone = "light",
                    style = "casual"
                )
            )
        ),
        today = null
    )

    private val homeSml = """
        Page {
          id: "home"
          title: "InnerNet"

          Column {
            alignment: "center"
            spacing: 24
            padding: 32

            Text { id: "appTitle" text: "InnerNet" style: "title" }
            Text { id: "tagline" text: "Free your mind. Connect with yourself." style: "subtitle" }
            Spacer { amount: 16 }
            Text { id: "questionIntro" text: "Which question will guide you today?" style: "question" }
            Column {
              spacing: 12
              Button { id: "q_who" text: "Who do you want to be today?" action: "goto_choose_avatar" }
              Button { id: "q_what" text: "What do you truly want?" action: "goto_focus_goals" }
              Button { id: "q_learn" text: "What do you want to learn today?" action: "goto_learning" }
              Button { id: "q_random" text: "Surprise me" action: "goto_random_question" }
            }
          }
        }
    """.trimIndent()
}
*/