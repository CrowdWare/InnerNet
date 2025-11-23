# InnerNet WebApp WordPress Plugin

Dieses Plugin bringt die gebaute InnerNet Webapp inklusive aller Assets mit und rendert sie unter `/innernet-app` im Vollbild – ohne WordPress-Menü, Header oder Footer.

## Inhalt
- `innernet-webapp.php` – Plugin-Logik, Route und Rendering.
- `app/` – gebaute Produktionsassets aus `build/dist/js/productionExecutable`.
- `content/` – Platz für zusätzliche SML/Asset-Dateien, die unter dem Plugin-Pfad aufgerufen werden können.

## Installation
1. Plugin-Verzeichnis `wordpress-plugin/innernet-webapp` zippen oder direkt nach `wp-content/plugins/innernet-webapp` kopieren.
2. In WordPress aktivieren. Die Aktivierung legt automatisch die Route `/innernet-app` an, erzeugt eine Seite `/innernet-webapp` (ohne Theme-Menüs/Footers) und leert die Rewrite-Regeln.
3. Aufrufen über `https://<deine-domain>/innernet-app` oder die erzeugte Seite `https://<deine-domain>/innernet-webapp`.

## Assets & Content aktualisieren
- Neue Webapp-Builds: erzeugen (`./gradlew jsBrowserProductionWebpack`) und den Inhalt von `build/dist/js/productionExecutable/` in `app/` ersetzen.
- Eigene Inhalte: Dateien nach `content/` legen. Im Browser stehen sie unter `window.INNERNET_CONTENT_BASE` zur Verfügung (z. B. `${window.INNERNET_CONTENT_BASE}mein.sml`).

## Hinweise
- Die HTML-Ausgabe enthält `<base href="<plugin>/app/">`, sodass alle relativen Pfade innerhalb des Plugins aufgelöst werden und keine WordPress-Themenbestandteile geladen werden.
- Die WordPress-Admin-Bar wird auf der App-Seite ausgeblendet.
