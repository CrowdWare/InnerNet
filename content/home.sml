Page {
  title: "InnerNet"
  padding: "16 16 16 0"

  Column {
    alignment: "center"
    spacing: 24
    padding: "16 16 16 32"

    Markdown { text: "# InnerNet"}
    Markdown { text: "**Free your mind**. **Connect with yourself.**" }
    Spacer { amount: 16 }
    Text { text: "Which question will guide you today?" }
    Column {
      spacing: 12
      Button { label: "model:who" link: "page:who"}
      Button { label: "What do you truly want?" link: "page:what"}
      Button { label: "What do you want to learn today?" link: "page:learn"}
      Button { label: "Surprise me" link: "page:surprise"}
      Button { label: "Page Funktion Call" onClick: "println(myPageFunction())"}
      Button { label: "Global Funktion Call" onClick: "println(randomNumber())"}
    }
  }
  Spacer { weight: 1}
  Text { text: "global:statusMessage" }
}
