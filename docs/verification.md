# Prüfbericht

## Mac-App-Store-Vorbereitung, 31. Oktober 2026

**Linux-Vorbereitung geprüft. Native Mac-Abnahme und Einreichung offen.**
Produktname Bookshelf by Appfabrik, Version 1.0, Apple-Build 1, erste Store-Texte
auf Deutsch. Die vorhandenen sechs UI-Sprachen bleiben erhalten.

| Prüfung des aktuellen Quellstands | Ergebnis |
| --- | --- |
| `./mvnw spotless:apply verify` mit Temurin 21.0.13+9 | 94 Tests ohne Fehler oder ausgelassene Fälle. Compiler-Lint, Enforcer, Spotless und SpotBugs bestanden |
| `python3 scripts/test_macos_release.py` | Fünf Tests bestanden. Architekturprüfung, fehlerhafte Archive und Trennung nativer SQLite-Bibliotheken geprüft |
| Isolierte Xvfb-GUI, sechs Sprachen | Aufnahme, Mindestfenster, Speicherung bei Datenbanksperre, Datenschutz, Sprachwechsel und separater Neustart bestanden |
| Deutscher JSON-Transfer | 32 GUI-Prüfungen bestanden |
| Deutscher Store-Bildentwurf | 24 GUI-Prüfungen bestanden. Vier echte PNGs in 2560 × 1600, ausschließlich synthetische Bücher |
| Manuelle Bildkontrolle | Sammlung, Bearbeitung, Suche und Exportdialog geprüft. Fehlermeldung und Speichern/Abbrechen im kleinen Editor sichtbar |
| Mac-Eingaben | SHA-256 aller gelieferten Eingaben und Mach-O-Architekturen der gepinnten JDK-/JavaFX-Dateien geprüft |
| Python- und Shell-Syntax, `git diff --check` | Bestanden |

Der erste GUI-Durchlauf fand ein Platzproblem im kleinen Buchdialog bei langer
Fehlermeldung. Das Formular ist jetzt scrollbar. Fehlermeldung und Aktionen
liegen außerhalb der Scrollfläche. Anschließend wurden Java- und alle GUI-
Prüfungen mit dem endgültigen Quellstand erneut erfolgreich ausgeführt.

Die Nachweise liegen unter `target/mac-preparation-*.log` und in den Manifesten
von `target/mac-preparation-gui-v2`, `target/mac-preparation-transfer-v2` und
`target/mac-preparation-store-v2`. Die Übergabe übernimmt diese Nachweise.
Persönliche JSON-Exporte werden weder gelesen noch übernommen. Der Nutzerdesktop
und bereits laufende App-Instanzen bleiben unberührt. Kein Linux-App-Image
installiert oder ersetzt. Deshalb wird keine neue Paket-GUI-Abnahme behauptet.

Die Mac-Helfer sind unter Linux implementiert und ihre plattformunabhängigen
Prüfungen ausgeführt. Cocoa-Kompilierung, signierte Ausführung, JIT, Sandbox-
Freigaben, Bibliotheksladung auf beiden Architekturen, Transporter und App Review
sind **nicht** ausgeführt. Der native Mac-Exportdialog benötigt weiterhin die
praktische Abnahme. Linux-Aufnahmen sind keine finalen Mac-Screenshots.
Anleitung: [Mac-Feldtexte](mac-app-store-fields.md) und
[Mac-Bau und Abnahme](mac-app-store-release.md).

## MSIX-Vorbereitung nur für BücherRegal, 31. Oktober 2026

Urteil bleibt **noch nicht bereit**. Der Nutzer hat noch keine App im Partner
Center angelegt. Die drei echten Produktidentitätswerte fehlen. Auf diesem
Ubuntu-PC wurde kein Windows-MSIX gebaut, signiert oder installiert.

Neu vorbereitet sind ein Windows-MSIX-Helfer, Manifest und Paketlogos,
Konfigurationsvorlage ohne erfundene Identität, Erstveröffentlichungstexte in
sechs Sprachen sowie Support- und Zertifizierungshinweise. Der Helfer baut ein
neues App-Image mit eingebettetem Java, setzt die echte Identität ein und nutzt
die reguläre MakeAppx-Paketvalidierung. Er startet weder App noch Tests. Seine
Windows-Ausführung ist offen. Die bestehende EXE-Paketierung bleibt separat.

Lokal tatsächlich ausgeführt: Paketlogos aus dem vorhandenen Motiv erzeugt.
Vollständige Temurin-/OpenJFX-Quellarchive, Temurin-Buildskripte und Herkunfts-
metadaten heruntergeladen und ihre SHA-256 für die Lieferung festgehalten.
Temurins Quellarchiv wurde mit der veröffentlichten SHA-256 abgeglichen.
Keine Windows-Binärdateien dadurch abgenommen. Die öffentliche Bereitstellung
der Quellen und die endgültige Windows-Lizenzinventur bleiben offen.

`scripts/prepare_store_release.py` erstellt eine interne Ordnerübergabe aus dem
committeten Quellstand. Sie enthält Store-Texte, Datenschutztexte aus den
Produktionsressourcen, Icons, Laufzeitquellen und die manuelle Anleitung.
Sie ist keine geprüfte Vorschau von `assemble.py`. Ihre Metadaten nennen
`storeReady: false`, `testsExecuted: false` und offene Windows-Abnahme.
Unversionierte persönliche JSON-Exporte werden nicht übernommen.

Auf Nutzerwunsch keine Tests, kein `verify`, keine GUI-Automation und kein
WACK-Lauf. Manuelle Schritte und benötigte Nachweise stehen in
[microsoft-store-release.md](microsoft-store-release.md). Insbesondere sind
Installation ohne vorhandenes Java, Offline-Betrieb, sechs Sprachen, JSON,
Datenpfad und Datenbestand nach einem MSIX-Update sowie finale Windows-Bilder
noch abzunehmen. Historische Prüfungen weiter unten gelten nur für ihren Stand.

## App-Icons auf den drei Desktop-Plattformen, 31. Oktober 2026

Vorhandene Motive in zusätzliche Fenstergrößen, Windows-ICO und macOS-ICNS
überführt. Fenster und Startfehler verwenden die App-Icons. Linux erhält einen
benutzerspezifischen Anwendungseintrag mit passender JavaFX-Fensterklasse und
Icons. Windows- und macOS-Pakethelfer verknüpfen die jeweiligen Icon-Dateien
explizit mit Launcher/Bundle und Installer.

Tatsächlich ausgeführt: Icon-Erzeugung mit Pillow, Formatter und Java-Paketbau
mit `-Dmaven.test.skip=true`. Protokoll: `target/desktop-icons-build.log`.
Vorhandene Linux-App-Images mit App-JAR, Icons, Desktop-Installer und Anleitung
aktualisiert. Die Linux-Desktop-Registrierung wurde für das aktuelle Benutzerkonto
ausgeführt. Keine vollständigen nativen Installer neu gebaut.

BücherRegal wurde auf Desktop 2 neu geöffnet. Sein Anwendungseintrag öffnet
weiterhin `target/collection-transfer-packaged/synthetic-data`. Datenbank und
Spracheinstellung beim Neustart bytegenau unverändert. Vor der Änderung war die
Fensterklasse vorhanden, ein passender Desktop-Eintrag jedoch nicht.

Auf Nutzerwunsch keine Tests, kein `verify` und keine automatisierten GUI-Prüfungen.
Manuelle Abnahme: App über die Ubuntu-Anwendungsübersicht starten, eigenes Icon
im Dock und beim Fensterwechsel ansehen, Dialoge öffnen und nach einem Neustart
die Zuordnung kontrollieren. Windows separat in Explorer, Startmenü und Taskleiste
prüfen, macOS separat im Finder, Dock und App-Umschalter. Native Windows-/macOS-
Pakete und Signierungen wurden hier nicht erzeugt oder geprüft.

## Exportname mit Uhrzeit, 31. Oktober 2026

Der native Speicherdialog schlägt jetzt einen Namen mit lokalem Datum und
Uhrzeit einschließlich Sekunden vor, beispielsweise `buecherregal-2026-10-31_09-25-00.json`.
Format: `uuuu-MM-dd_HH-mm-ss`, sprachunabhängig und ohne Doppelpunkte für Windows.
Der Zeitstempel wird beim Öffnen des Dialogs ermittelt. Manuell gewählte Namen,
Exportinhalt und bestehender Überschreibschutz bleiben erhalten. Zwei Aufrufe
innerhalb derselben Sekunde können weiterhin denselben Vorschlag erzeugen.

Formatter und Paketbau mit `-Dmaven.test.skip=true` erfolgreich.
Buildprotokoll: `target/export-filename-build.log`. Vorhandenes Linux-App-Image
mit aktuellem App-JAR und aktualisierter Anleitung versehen. Keine Tests,
kein `verify` und keine automatischen GUI-Prüfungen ausgeführt.

Manuelle Abnahme: Exportdialog öffnen, Datum sowie Stunden, Minuten und Sekunden
im vorgeschlagenen Namen prüfen, abbrechen und nach einigen Sekunden erneut
öffnen. Der Zeitanteil muss aktualisiert werden. Einen eigenen Namen weiterhin
wählen können. Keine automatische Test- oder GUI-Ausführung auf Nutzerwunsch.

## CJK-Schriftdarstellung, 31. Oktober 2026

Der gemeldete Beispieltitel „Bücher 日本語 📚“ ist in der synthetischen
BücherRegal-Datenbank vollständig gespeichert. Die drei Kästchen betreffen die
Darstellung. Alle fünf Apps verwenden jetzt eine app-lokal mitgelieferte,
explizit geladene Schrift Noto Sans CJK JP 2.004 mit Regular und Bold.
Die Änderung gilt für Tabellen, Eingaben und die übrige gestylte Oberfläche.
Gespeicherte Texte und Sprachwahl werden nicht umgeschrieben.

Die eingesehene native Linux-Schriftauswahl von OpenJFX 21.0.11 filtert bei der
Fallback-Ermittlung auf TrueType. Die hier installierten Noto-CJK-Schriften
verwenden CFF-Outlines. Die primäre, direkt geladene OTF-Schrift umgeht diesen
Fallback für die enthaltenen Zeichen. Andere Unicode-Zeichen und die Darstellung
von Emojis hängen weiterhin von der JavaFX-Ersatzschrift des Zielsystems ab.

Quelle: [OpenJFX 21.0.11, fontpath_linux.c](https://github.com/openjdk/jfx21u/blob/21.0.11%2B4/modules/javafx.graphics/src/main/native-font/fontpath_linux.c).
Die Einbindung nutzt die öffentliche API
[Font.loadFont](https://openjfx.io/javadoc/21/javafx.graphics/javafx/scene/text/Font.html).

Formatter und Java-Kompilierung mit Paketbau erfolgreich. Pro App wurde
`./mvnw spotless:apply package -Dmaven.test.skip=true` mit dem eigenen
Temurin 21.0.13+9 ausgeführt. Protokoll: `target/ui-font-build.log`.
Die vorhandenen Linux-App-Images wurden mit dem neuen App-JAR und den
zusätzlichen Lizenzhinweisen aktualisiert. Die Java-Laufzeit blieb unverändert.

Auf Nutzerwunsch keine Tests, kein `verify` und keine automatisierte GUI-Abnahme.
Manuell abzunehmen: den obigen Beispieltitel in Tabelle und Bearbeitungsdialog
lesen, zusätzlich Umlaute und Akzente in den sechs UI-Sprachen ansehen und
Dialoggrößen sowie fette Überschriften kontrollieren. In den anderen Apps dafür
nur eigene temporäre Beispieldaten verwenden. Windows und macOS bleiben offen.

## Roter Löschfokus und Übernahme in die Java-Reihe, 31. Oktober 2026

Destruktive Schaltflächen verwenden bei Fokus einen roten Rahmen. Bearbeiten
und andere Aktionen behalten ihre App-Farbe. Doppelte, identische
`action.close`-Einträge wurden aus den sechs Sprachkatalogen entfernt.
Datenschutzdialog, UI-Verbesserungen und lokale Paketvorbereitung wurden in
GarnBox, StrickProjekt, NähPlan und KostümPlan jeweils app-lokal übernommen.
Die Erklärungen berücksichtigen deren eigene Foto- und Projektfunktionen.
JSON-Import/-Export bleibt in diesem Übernahmeschritt auf BücherRegal begrenzt.

Formatter und Paketbau mit `-Dmaven.test.skip=true` bestanden. Keine Tests,
kein `verify` und kein GUI-Prüflauf, entsprechend dem fortgeltenden Nutzerwunsch.
Die App wurde mit derselben Testsammlung auf Desktop 2 neu geöffnet.
Datenbank und Spracheinstellung blieben dabei bytegenau unverändert.
Manuell noch abzunehmen: Eintrag auswählen, „Löschen“ mit Tab fokussieren und
roten Rahmen kontrollieren. „Bearbeiten“ behält seinen grünen Fokusrahmen.
Die aktuellen sechs Datenschutztexte liegen unter `build/releases/privacy/2026-10-31`.
Buildprotokoll: `target/portfolio-privacy-build.log`. Eine bereits vorhandene
JSON-Exportdatei des Nutzers im Projektverzeichnis bleibt unberührt und außerhalb
des Commits.

## Sichtbares Versetzen beim Öffnen vermeiden, 31. Oktober 2026

Der Nutzer bestätigt die Zentrierung, sieht aber die Bewegung von der anfänglichen
zur endgültigen Position. Der Dialog bleibt deshalb beim ersten Öffnen unsichtbar,
bis Größenkorrekturen abgeklungen sind und die Position angewendet wurde.
Nach der Zentrierung wird er mit weiteren 100 ms Abstand sichtbar. Neue
Größenmeldungen verschieben diesen Zeitpunkt. Beim Schließen werden beide Timer
gestoppt. Die Größe bleibt erhalten.

Auf Nutzerwunsch nur Formatter und Paketbau mit `-Dmaven.test.skip=true`, keine
Tests und kein GUI-Prüflauf. Der Build besteht. Protokoll:
`target/privacy-reveal-manual-build.log`. Die visuelle Abnahme übernimmt der
Nutzer. Die aktualisierte App wird mit derselben Testsammlung auf Desktop 2
geöffnet. Dies ist eine lokale Startaktion und keine Plattformabhängigkeit im App-Code.

## Erneute Dialogpositionierung zur Nutzerprüfung, 31. Oktober 2026

Die Größe bleibt erhalten. Die Zentrierung wartet nach dem Öffnen und nach
Größenänderungen jeweils 150 ms, damit native Größenkorrekturen berücksichtigt
werden. Sie verwendet die dann gemeldete Fenstergröße und begrenzt die Position
auf den sichtbaren Bildschirmbereich.

Auf ausdrücklichen Nutzerwunsch wurden keine Tests, kein GUI-Prüflauf und kein
`verify` ausgeführt. Formatter und Paketbau mit `-Dmaven.test.skip=true` liefen
erfolgreich. Die visuelle Abnahme übernimmt der Nutzer in der geöffneten App.
Buildprotokoll: `target/privacy-position-manual-build.log`.
Die folgenden Prüfungen beziehen sich auf die jeweiligen früheren Stände.

## Datenschutzdialog vergrößert, 31. Oktober 2026

Der Datenschutzdialog behält die zuletzt gewählte Größenkonfiguration:
bevorzugter Inhalt 780 × 660, Mindestfenster 560 × 420. Die tatsächliche
Darstellung hängt zusätzlich vom Fensterrahmen und Fenstermanager ab.
Auf Nutzerwunsch bestimmt wieder das System die anfängliche Position.
Es gibt keine eigene Zentrierung und keine programmatischen X-/Y-Vorgaben.

`./mvnw verify` besteht mit 91 Tests ohne Fehler oder ausgelassene Fälle,
einschließlich Compiler-Lint, Enforcer, Spotless und SpotBugs. Die deutsche
GUI-Prüfung mit Datenschutzanzeige, Kopieren, Scrollen, Escape, Schließen und
Sprachneustart besteht. Nachweise liegen unter `target/privacy-size-final-gui`.
Der Buildnachweis liegt in `target/privacy-size-final-verify.log`.

Das neue App-JAR unterscheidet sich vom vorherigen geprüften Paket ausschließlich
in `PrivacyDialog.class`. Es wurde im vorhandenen Linux-App-Image ersetzt und
der Launcher mit derselben Testsammlung sichtbar neu geöffnet. Datenbank und
Spracheinstellung blieben bytegenau unverändert. Der Nachweis liegt unter
`target/privacy-size-desktop-final.json`. Die bisherigen Vorschau-Lieferungen
bleiben ihrem früheren Quellcommit zugeordnet.

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
