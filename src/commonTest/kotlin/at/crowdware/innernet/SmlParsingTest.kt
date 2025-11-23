package at.crowdware.innernet

import at.crowdware.innernet.state.InnerNetState
import at.crowdware.innernet.state.Profile
import at.crowdware.innernet.state.SmlStateCodec
import at.crowdware.innernet.state.Avatar
import at.crowdware.innernet.state.Visual
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmlParsingTest {
    @Test
    fun roundTripsState() {
        val state = sampleState()
        val encoded = SmlStateCodec.encode(state)
        val decoded = SmlStateCodec.decode(encoded)
        assertEquals(state.profile?.nickname, decoded.profile?.nickname)
        assertEquals(state.avatars.first().visual?.hairColor, decoded.avatars.first().visual?.hairColor)
    }

    private fun sampleState() = InnerNetState(
        profile = Profile("VisionSeeker"),
        avatars = listOf(
            Avatar(
                id = "visionary",
                level = 2,
                xp = 22,
                visual = Visual(
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
}
