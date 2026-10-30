# Prüfbericht

## Lokale Store-Vorbereitung und Datenschutz, 30. Oktober 2026

Urteil für einen Microsoft-Store-Release: **noch nicht bereit**. Die lokale
Vorbereitung ist geprüft. Ein aktuelles Windows-Paket, Installation, Update,
MSIX-Identität, Zertifizierung und endgültige Windows-Aufnahmen sind nicht belegt.

Unter „Info“ ist die vollständige Datenschutzerklärung offline in allen sechs
Sprachen erreichbar. Dialog und Store-Textexport verwenden dieselben Ressourcen.
Der Dialog ist schreibgeschützt, scrollbar und kopierbar. Escape und „Schließen“
führen zur unveränderten Sammlung zurück. Die Texte beschreiben lokale Speicherung,
JSON-Dateien, unabhängige Sicherungen, Löschung und freiwillige Supportkontakte.

| Tatsächlich ausgeführte Prüfung | Ergebnis |
| --- | --- |
| `./mvnw verify` auf Linux x64 mit Temurin 21.0.13+9 | 91 Tests, keine Fehler, keine ausgelassenen Fälle. Compiler-Lint, Enforcer, Spotless und SpotBugs bestanden |
| 18 echte GUI-Prozesse in sechs Sprachen | 504 Prüfbedingungen bestanden, einschließlich Datenschutz, kleinen Fenstern, Sprachwechsel und Neustart |
| Deutscher JSON-Ablauf mit nativen Linux-Dateidialogen | 32 Prüfbedingungen bestanden, einschließlich Abbruch, ungültiger Datei, Duplikaten, erhaltenen IDs und SQLite-Sperre |
| Linux-App-Image neu gebaut und sichtbar gestartet | 19 Paketprüfungen bestanden. Mitgelieferte JVM aus Prozessbibliotheken belegt, `JAVA_HOME` entfernt, `PATH=/usr/bin:/bin` |
| Paketierte Oberfläche | Unicode-Buch mit mehrzeiliger Notiz gespeichert. Deutsche und englische Datenschutzerklärung vollständig kopiert und mit Store-Textexport verglichen. Escape, Schließen und Neustart bestanden |
| Datenhaltung im Paket | Datenbank bei Datenschutzanzeige und Sprachneustart bytegenau unverändert. Spracheinstellung bei Datenschutzanzeige bytegenau unverändert |
| Paketinhalt | Aktuelles App-JAR, sechs Datenschutzressourcen, JavaFX und `jdk.localedata`, Laufzeitabhängigkeiten, Lizenztexte, Bedienungsanleitung und originale JDK-Herkunftsdatei geprüft |
| Bildvorschau | 24 echte Linux-Aufnahmen in 1920 × 1080 erneuert und visuell kontrolliert. Zusätzlich Datenschutzdialoge in normaler und kleiner Größe geprüft |
| Store-Texte und Icons | Textlängen aller sechs Sprachen innerhalb der dokumentierten Grenzen. Kurztexte passen in 270 Zeichen. Store-PNG mit 300 × 300 Pixeln und ICO mit sechs Größen von 16 bis 256 Pixeln geprüft |
| Datenschutztextexport | Sechs UTF-8-Dateien aus denselben Ressourcen erzeugt. Schutz vor Überschreiben eines vorhandenen Ausgabeordners durch JUnit geprüft |

Die app-lokale Temurin-Installation wurde aus dem offiziellen Archiv mit der
veröffentlichten SHA-256-Prüfsumme geprüft:

```text
OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz
d7f5d5cf865a2902b70ff549b97586a7d3f234e016f041ab447ea740c7d66d1c
```

Andere Java-Installationen wurden nicht ersetzt. Die Maven-Regeln und beide
GitHub-Workflows sind auf dieselbe Version und Buildnummer festgelegt. Die
geänderten Workflows wurden nicht gestartet. Der ältere belegte CI-Lauf mit
33 Tests ist in [packaging.md](packaging.md) eingeordnet.

Die Textlängen wurden gegen die aktuelle
[Microsoft-Anleitung für MSIX-Store-Einträge](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msix/add-and-edit-store-listing-info)
geprüft. Das Kurztextfeld erlaubt 1.000 Zeichen, manche Ansichten zeigen
zunächst 270. Die gewählten kürzeren Texte bleiben vollständig sichtbar.

Der Hardlink-Schutztest hängt nicht mehr von POSIX-Dateiattributen ab. Er wird
damit auch auf Windows ausgeführt, sobald dort die aktuelle Suite läuft.
Fehlende Rechte zum Erzeugen symbolischer Links werden gesondert als
ausgelassener Test ausgewiesen. Unter Linux bestanden beide Linktests.
Der sichere JSON-Export wurde nicht abgeschwächt. FAT32 und exFAT bleiben als
direkte Exportziele ungeeignet. Die übersetzten Fehler- und Store-Texte sowie
die Anleitung erklären den Weg über einen lokalen Export mit anschließendem Kopieren.

Aktuelle Nachweise in ignorierten Verzeichnissen:

```text
target/store-preparation-verify-final.log
target/store-preparation-package.log
target/store-preparation-all-languages
target/store-preparation-transfer
target/store-preparation-packaged-v2
target/store-preparation-privacy
target/store-preparation-visual-review
```

Der erste separate Pakettest verwendete eine unvollständige Tastensequenz zum
Öffnen des Menüs. Mit Öffnen per Leertaste, anschließend Pfeil abwärts und Enter
bestand der vollständige Ablauf. Dafür war keine App-Änderung erforderlich.
Alle Oberflächenprüfungen verwendeten eigene Xvfb-Prozesse und ausschließlich
ausdrücklich markierte synthetische Daten. Nutzerfenster wurden nicht bedient.

Die neue Vorschau wird als Ordner unter `build/releases/international/`
ausgegeben. Ihr Manifest ordnet die Aufnahmeprüfsummen dem sauberen lokalen
Quellcommit zu. Sie enthält Store-Texte, 24 Bilder, Branding, Datenschutztexte
und Nachweise. Es wird kein ZIP erzeugt. Frühere Lieferungen bleiben erhalten.

Nur vorbereitet sind der explizite Windows-ICO-Pfad und die aktualisierte
PowerShell-Paketierung. MSIX für den Store und eine kostenlose GitHub-EXE sind
als getrennte Vertriebswege dokumentiert. Die MSIX-Paketierung selbst fehlt noch.
Die nächsten Release-Schritte stehen priorisiert in [packaging.md](packaging.md).
Es erfolgten kein Push, Workflow-Start, Upload oder Veröffentlichung.

## JSON-Export und Import, 29. Oktober 2026

„Neu laden“ wurde durch „Exportieren“ und „Importieren“ ersetzt. Beide Aktionen,
Bestätigungen und Fehlerhinweise sind in allen sechs Sprachen vorhanden.
Exportiert wird die gesamte gespeicherte Sammlung einschließlich aller vier
Textfelder. Der Import ergänzt fehlende Exemplare in einer Transaktion und
erhält bestehende IDs. Erneutes Importieren derselben Datei erzeugt keine
weiteren identischen Exemplare. Bestehende unterschiedliche Notizen bleiben erhalten.

88 JUnit-Testfälle bestehen ohne Fehler oder ausgelassene Fälle. Compiler-Lint,
Enforcer, Spotless und SpotBugs bestehen. Die neuen Fälle prüfen Unicode und
mehrzeilige Notizen, vollständige Wiederherstellung, leere Sammlungen,
unabhängige identische Exemplare, wiederholten Import, späten SQL-Fehler mit
Rollback, echte SQLite-Sperren, ungültige JSON-Typen, doppelte Schlüssel,
beschädigtes UTF-8, unbekannte Versionen, Größenlimits und geschützte Dateiziele.
Bestehende Exporte bleiben bei Fehlern erhalten. Temporäre Dateien werden entfernt.

18 echte GUI-Prozesse bestehen mit 336 Prüfbedingungen. Die 24 erneuerten
Aufnahmen wurden in allen sechs Sprachen visuell geprüft. Kleine Hauptfenster,
Editoren, Suche, Sprachwahl und Neustart bleiben bedienbar. Der zusätzliche
deutsche Import-/Export-Ablauf besteht mit 30 Prüfbedingungen. Er bedient native
Dateidialoge, exportiert trotz leerem Suchergebnis alle Bücher und prüft Abbruch,
ungültige Dateien, sichere Importbestätigung, Duplikate, erhaltene IDs und eine
SQLite-Sperre mit erfolgreichem Wiederholungsversuch. Python hat den erzeugten
Export unabhängig als JSON gelesen und alle Felder geprüft.

Das neue App-Image enthält Gson, Error Prone Annotations und deren Lizenztexte.
Der eigenständige Launcher wurde auf einem eigenen Xvfb-Display ohne JAVA_HOME
und mit PATH=/usr/bin:/bin geprüft. Zwei synthetische Bücher wurden über den
nativen Dialog exportiert, die JSON-Datei unabhängig gelesen und anschließend
eine Datei mit einem zusätzlichen Buch importiert. Die beiden vorhandenen
Datensätze blieben vollständig erhalten. Das aktualisierte Paket wurde auch auf
dem Nutzerdesktop mit derselben Testsammlung neu geöffnet. Die Datenbank war
vor und nach diesem Neustart bytegenau identisch.

Aktuelle Nachweise in ignorierten Verzeichnissen:

```text
target/collection-transfer-verify-v2.log
target/collection-transfer-all-languages
target/collection-transfer-native
target/collection-transfer-packaged
target/collection-transfer-user-restart.json
```

Frühere Lieferordner bleiben unverändert ihrem damaligen Quellcommit zugeordnet.
Die folgenden Abschnitte dokumentieren die vorherigen Abnahmen. Native Windows-
Dateidialoge und Dateisystemoperationen bleiben Teil der Windows-Abnahme.

## Auswahlrahmen entfernt, 29. Oktober 2026

Der zusätzliche grüne Rahmen der fokussierten Tabellenzeile ist transparent.
Die grüne Auswahlfläche bleibt erhalten. Der echte deutsche GUI-Ablauf mit
angeklickter Zeile, Editor, Suche und Sprachwahl besteht auf einem eigenen
Xvfb-Bildschirm. Die neue Übersicht wurde visuell auf den rahmenlosen linken
Rand geprüft. Nachweise liegen unter `target/selection-border-capture`.

`./mvnw verify` besteht mit 58 Tests, Compiler-Lint, Enforcer, Spotless und
SpotBugs. Das aktualisierte App-JAR unterscheidet sich inhaltlich ausschließlich
in `books.css` vom vorherigen Paket. Der Launcher wurde mit derselben manuellen
Testsammlung neu geöffnet. Die Datenbank blieb dabei bytegenau unverändert.
Die frühere internationale Lieferung bleibt ihrem damaligen Quellcommit zugeordnet.

## Internationalisierung, 29. Oktober 2026

BücherRegal 0.1.0 wurde als Pilot in de-DE, en-US, es-ES, fr-FR, it-IT und pt-BR
internationalisiert. Die nachfolgenden Abschnitte zum 25. Oktober dokumentieren
den früheren Entwicklungsstand. Für diese Erweiterung gelten die folgenden
Prüfergebnisse.

58 JUnit-Testfälle bestehen. Compiler-Lint mit Warnungen als Fehlern,
Maven Enforcer, Spotless und SpotBugs bestehen ebenfalls. Hinzugekommen sind
vollständige ResourceBundles mit kompatiblen Platzhaltern, alle Speicherfehler,
regionale Zahlenformate und Pluralformen, Sprachauflösung und Rückfall,
Apostrophe und Unicode sowie dauerhafte Spracheinstellungen mit Fehlerfällen.
Beim Sprachwechsel wurde die bestehende Datenbank bytegenau unverändert geprüft.

Je Sprache wurden drei neue JVM-Prozesse mit der produktiven BooksApplication
auf einem isolierten Xvfb-Bildschirm ausgeführt. Alle 18 Prozesse bestanden.
Der GUI-Helfer bedient die echten Fenster über JavaFX Robot mit Maus und Tastatur.

| Prüfung | Ergebnis |
| --- | --- |
| Alle sechs gespeicherten Sprachen bei japanischer Systemsprache | Richtige Oberfläche beim echten Prozessstart |
| Übersicht, Bearbeiten, Suche, Sprachwahl | 24 originale PNG-Aufnahmen in 1920 × 1080 erstellt |
| Leerer Titel | Passender übersetzter Feldhinweis |
| Akzente, Apostrophe, Unicode und 30-zeilige Notiz | Vollständig eingegeben und gespeichert |
| Escape bei geändertem Entwurf | Weiterbearbeiten als sichere Vorauswahl, Entwurf bleibt erhalten |
| Exklusive SQLite-Sperre beim Speichern | Übersetzter Fehler, Entwurf bleibt vollständig erhalten |
| Sperre lösen und erneut speichern | Genau ein neues Buch gespeichert |
| Kleine Hauptfenster und Eingabedialoge | Aktionen bleiben im Fenster, Beschriftungen passen |
| Löschen abbrechen | Datensatz bleibt bestehen |
| Einstellungsziel durch Verzeichnis blockiert | Fehler wird übersetzt angezeigt, Auswahl bleibt erhalten |
| Ziel freigeben und Sprache erneut speichern | Speicherung und Neustarthinweis erfolgreich |
| Neuer Prozess mit geänderter Sprache | Neue Sprache und erhaltene Unicode-Buchdaten bestätigt |

Alle 24 Vorschau-Aufnahmen wurden visuell anhand der Sprachübersichten geprüft.
Zusätzlich wurden die ursprünglichen großen Aufnahmen für Übersicht, Editor,
Sprachwahl und einen kleinen Fehlerdialog angesehen. Die Bilder enthalten nur
synthetische Bücher. Ein Markenname auf Deutsch ist beabsichtigt.

Die unabhängige Linux-App-Image-Abnahme wurde mit dem paketierten Launcher,
entferntem JAVA_HOME, eigenen temporären Daten und separatem Xvfb durchgeführt.
Französische Oberfläche und Pflichtfeldhinweis, Unicode-Eingabe, mehrzeilige
Notiz, Speichern und englischer Prozessneustart mit bytegleich erhaltener
Datenbank wurden geprüft. Die Prozessbibliotheken bestätigen die eingebettete
JVM und JavaFX. Die finale Laufzeit enthält zusätzlich `jdk.localedata` für
regionale Formatierung und Sortierung.

Befehle und reproduzierbare GUI-Schritte stehen in
[internationalization.md](internationalization.md). Die Lieferdatei enthält
GUI-Protokolle, Screenshots, Paketprüfnachweise und Herkunftsmanifest.
Die Bildlieferung ist ausdrücklich eine Linux-Vorschau. Windows-Paketierung,
native Windows-Abnahme und endgültige Windows-Store-Aufnahmen bleiben offen.
Es wurde weder gepusht noch eine Store-Einreichung ausgeführt.

## Ursprüngliche Basisprüfung

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
