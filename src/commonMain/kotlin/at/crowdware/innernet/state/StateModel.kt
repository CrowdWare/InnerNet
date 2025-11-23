package at.crowdware.innernet.state

data class InnerNetState(
    val version: Int = 1,
    val profile: Profile?,
    val avatars: List<Avatar>,
    val today: Today?,
    val answers: List<Answer> = emptyList()
)

data class Profile(val nickname: String)

data class Avatar(
    val id: String,
    val level: Int,
    val xp: Int,
    val visual: Visual?
)

data class Visual(
    val gender: String?,
    val hairColor: String?,
    val hairLength: String?,
    val eyeColor: String?,
    val skinTone: String?,
    val style: String?
)

data class Quest(
    val id: String,
    val title: String,
    val xp: Int,
    val done: Boolean
)

data class Today(
    val date: String,
    val selectedAvatarId: String?,
    val quests: List<Quest>
)

data class Answer(
    val date: String,
    val question: String,
    val text: String
)
