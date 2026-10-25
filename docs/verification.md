# Prüfbericht

Stand: 25. Oktober 2026, BücherRegal 0.1.0.

## Ausgeführte automatische Prüfungen

Ubuntu Linux x64, Eclipse Temurin JDK 21.0.11, Maven Wrapper 3.9.16,
JavaFX 21.0.11 und SQLite JDBC 3.53.4.0.

```bash
./mvnw spotless:apply
./mvnw verify
bash -n scripts/run.sh scripts/package.sh
git diff --cached --check
```

33 JUnit-Testfälle bestehen. Compiler-Lint, Maven Enforcer, Spotless und
SpotBugs bestehen. Die JUnit-Tests umfassen:

- Leere und ausschließlich aus Leerraum bestehende Titel, einschließlich
  Unicode-Leerraum. Optionale Felder und mehrzeilige Notizen.
- Anlegen, Bearbeiten und Löschen in einer echten temporären SQLite-Datei.
- Wiederöffnen der Datenbank mit unveränderten IDs und gespeicherten Inhalten.
- Gleichnamige Bücher mit unabhängigem Bearbeiten und Löschen.
- Suche in allen vier Feldern, Groß- und Kleinschreibung, Umlaute und
  Unicode-Normalisierung. Leere Suche und Suche ohne Treffer.
- SQL-Sonderzeichen als Daten, lange Notizen und Unicode-Inhalte.
- Datenbanksperren, fehlende Einträge, beschädigte Datenbank, ungültiges
  Datenverzeichnis und eine unbekannte neuere Schema-Version.
- Windows-, macOS- und Linux-Datenpfade mit synthetischer Umgebung.

## Ausgeführte Oberflächenprüfung

Die JavaFX-Oberfläche wurde zunächst auf dem Linux-Desktop gestartet und
anschließend auf einem separaten Xvfb-Bildschirm über Tastatur und Maus bedient.
Die Ansichten wurden als Screenshots kontrolliert. Der Datenbankinhalt wurde
ergänzend gelesen. Es wurden ausschließlich synthetische Bücher in isolierten
Verzeichnissen unter `target` verwendet.

| Ablauf | Ergebnis |
| --- | --- |
| Leere Sammlung öffnen | Einladung zum ersten Buch sichtbar |
| Leeren Titel speichern | Feldhinweis sichtbar, kein Datenbankeintrag |
| Buch mit allen Feldern anlegen | Texte und mehrzeilige Notiz korrekt gespeichert |
| Eintrag bearbeiten | Gespeicherter Eintrag behält seine ID |
| Über einen Namen in der Notiz suchen | Treffer unabhängig von Groß- und Kleinschreibung |
| Unbekannten Suchbegriff eingeben | Eigener Hinweis für fehlende Treffer |
| Suche leeren | Gesamte Sammlung wieder sichtbar |
| Escape und Schließen bei Änderungen | Rückfrage, „Weiterbearbeiten“ als sichere Vorauswahl |
| Änderungen verwerfen | Gespeicherter Eintrag unverändert |
| SQLite während Speichern sperren | Dialog und Eingaben bleiben erhalten, kein Erfolg vorgetäuscht |
| Sperre lösen und erneut speichern | Eingaben erfolgreich gespeichert |
| Fehlermeldung bei 480 × 590 Pixeln | Vollständiger Text und Schaltflächen sichtbar |
| App schließen und erneut starten | Gespeicherte Einträge vorhanden |
| Gleichnamiges Buch anlegen | Separate ID, unabhängige Bearbeitung und Löschung |
| Löschbestätigung abbrechen | Eintrag bleibt erhalten |
| SQLite während Löschen sperren | Eintrag bleibt sichtbar und gespeichert |
| Lange Fehlermeldung im Hauptfenster | Vollständiger Hinweis und lesbare Schaltfläche „Neu laden“ |
| Löschen nach Freigabe bestätigen | Nur der ausgewählte Eintrag entfernt |
| Letzten Eintrag löschen | Einladung zur leeren Sammlung wieder sichtbar |

Die Datenbanksperre wurde mit einer zweiten SQLite-Verbindung und einer
exklusiven Transaktion nur auf der synthetischen Testdatenbank erzeugt.
Die Screenshot-Beispiele in der README wurden über die App angelegt.

## Paketprüfung

```bash
./scripts/package.sh app-image
./target/app-image/BuecherRegal/bin/BuecherRegal --version
```

Das Linux-x64-App-Image wurde gebaut und tatsächlich mit sichtbarer Oberfläche
ausgeführt. Ein weiterer Start mit entferntem `JAVA_HOME` und `PATH=/nonexistent`
funktionierte ebenfalls. Die geladenen Prozessbibliotheken bestätigen die
Verwendung der mitgelieferten `lib/runtime/lib/server/libjvm.so`.
JavaFX ist als Modul in dieser Laufzeit enthalten. Die Versionsausgabe lautet
`BücherRegal 0.1.0`. Die Datenbank wurde nach dem Paket-Neustart wieder angezeigt.

## Noch offen

Windows-EXE und macOS-DMG sind vorbereitet, aber auf diesem Linux-Rechner
weder erzeugt noch nativ ausgeführt worden. Die PowerShell-Paketierung und die
GitHub-Workflows wurden hier nicht auf ihren Zielplattformen ausgeführt.

Native Installation, Deinstallation ohne Datenverlust, Updates, Skalierung,
Tastenkürzel und Plattform-Sicherheitsdialoge benötigen die Windows- und
Mac-Abnahme. Die Linux-Prüfung ersetzt diese nicht.

Microsoft-Store- und Mac-App-Store-Schritte sind in `packaging.md` aufgeführt.
Es gibt noch keine geprüfte Store-Version und keine Veröffentlichung.
