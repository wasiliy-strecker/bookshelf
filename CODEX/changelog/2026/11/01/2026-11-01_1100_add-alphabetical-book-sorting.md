# Alphabetische Sortierung für Bookshelf by Appfabrik

Buchtitel, Autor und Aufbewahrungsort lassen sich über die Überschriften
aufsteigend und absteigend sortieren. Permanente Richtungspfeile entsprechen
dem Verhalten der anderen Java-Apps. Die Sortierung folgt der App-Sprache,
ignoriert Groß-/Kleinschreibung und bleibt bei Aktualisierungen aktiv.
Die Buchauswahl und zugehörige Notiz bleiben beim Sortieren erhalten.

Vorheriger Sicherungscommit: `8ed5974`. Implementierung, Bedienungsanleitung
und Prüfbericht bleiben app-lokal. Persönliche JSON-Exporte bleiben unversioniert.

Verifiziert mit Temurin 21.0.13+9 und isoliertem Xvfb:
`./mvnw --batch-mode --no-transfer-progress spotless:apply verify -Dbuecherregal.uiTests=true -Dprism.order=sw`.
107 Tests bestanden. Sieben neue GUI-Fälle prüfen Spaltenklicks in allen sechs
Sprachen sowie Suche, Auswahl und Bearbeitung. Compiler-Lint, Enforcer,
Spotless und SpotBugs bestanden. Synthetische GUI-Aufnahme visuell geprüft.

Das bestehende lokale Linux-App-Image enthält das verifizierte JAR und die
aktualisierte Anleitung. Die App ist mit ihrem bisherigen Datenverzeichnis
auf dem zweiten Desktop geöffnet. Kein Push oder Store-Release.
