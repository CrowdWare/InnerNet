package at.crowdware.innernet.ui

sealed interface UiNode {
    val id: String?
}

data class Page(
    override val id: String?,
    val title: String?,
    val children: List<UiNode>
) : UiNode

data class Column(
    override val id: String?,
    val alignment: String?,
    val spacing: Int?,
    val padding: Int?,
    val children: List<UiNode>
) : UiNode

data class Grid(
    override val id: String?,
    val columns: Int?,
    val spacing: Int?,
    val children: List<UiNode>
) : UiNode

data class Text(
    override val id: String?,
    val text: String?,
    val style: String?
) : UiNode

data class Button(
    override val id: String?,
    val text: String?,
    val action: String?
) : UiNode

data class Spacer(
    override val id: String?,
    val amount: Int?
) : UiNode

data class Dropdown(
    override val id: String?,
    val label: String?,
    val options: List<String>
) : UiNode

data class AvatarCard(
    override val id: String?,
    val title: String?,
    val subtitle: String?,
    val action: String?
) : UiNode
