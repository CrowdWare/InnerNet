Page {
  id: "home_ls_en"
  title: "InnerNet"

  Row {
    spacing: 16
    padding: 24
    Column {
      spacing: 16
      Markdown { id: "appTitle" text: "# InnerNet" }
      Markdown { id: "tagline" text: "**Free your mind.** **Connect with yourself.**" }
      Markdown { id: "questionIntro" text: "Which question will guide you today?" }
    }
    Column {
      spacing: 12
      Button { id: "q_who" text: "Who do you want to be today?" action: "goto_choose_avatar" }
      Button { id: "q_what" text: "What do you truly want?" action: "goto_focus_goals" }
      Button { id: "q_learn" text: "What do you want to learn today?" action: "goto_learning" }
      Button { id: "q_random" text: "Surprise me" action: "goto_random_question" }
    }
  }
}
