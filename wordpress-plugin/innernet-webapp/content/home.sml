Page {
  title: "InnerNet"
  padding: "16 16 16 0"

  Column {
    alignment: "center"
    spacing: 24
    padding: "16 16 16 32"

    Markdown { text: "string:title"}
    Markdown { text: "string:subLabel" }
    Spacer { amount: 16 }
    Text { text: "string:question" }
    Column {
      spacing: 12
      Button { label: "string:who" link: "page:who"}
      Button { label: "string:what" link: "page:what"}
      Button { label: "string:learn" link: "page:learn"}
      Button { label: "string:surprise" link: "page:surprise"}
    }
  }
  Spacer { weight: 1}
  Text { text: "global:statusMessage" }
}
