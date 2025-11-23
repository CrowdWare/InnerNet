# Hub-Idee (Pinning as a Service)

## Konzept
- Hubs/Pubs als optionale Replikationspunkte: Betreiber pinnen/repinnen Daten ihrer Gäste.
- Hub-Betreiber nutzt einen bezahlten Pinning-Provider (z. B. Pinata), hält den Dienst damit am Leben.
- Nutzer können Hubs beitreten/verlassen (kein Lock-in), zusätzlich eigene Pins setzen.

## Nutzen
- Betreiber: Community-Bindung, Sichtbarkeit, bietet stabilen Service.
- Nutzer: Verlässliches Pinning/Weiterverteilung, persönliche Betreuung, einfache Einrichtung.

## Umsetzungsideen
- Hub als Pinning-Backend: API-Key/Endpoint konfigurierbar, Re-Pin-Funktion.
- Opt-in-Flow: "Pin über diesen Hub sichern"; Hub pinnt/repinnt CIDs automatisch.
- Redundanz: Kombination aus Hubs + eigenem Pinning/Free-Tier.
- Betreiber-Tools: Setup, Monitoring, Quotas/Fair-Use.

## Prinzipien
- Austauschbarer Provider (Pinata o. Ä.), Migration/Re-Pin leicht möglich.
- Keine Fachbegriffe für Nutzer ("Sichern", "immer erreichbar", statt "Pin/CID").
- Opt-out jederzeit möglich.
