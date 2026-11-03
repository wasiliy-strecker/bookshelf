# Internationalisierung und Linux-Vorschau

Die App-Standardsprache ist Englisch, auch auf einem deutschen System.
Gespeicherte Sprachwahlen bleiben erhalten. Die erste Store-Veröffentlichung
verwendet de-DE für Beschreibungen, Bildtexte und Release-Hinweise. Diese
Store-Sprache bestimmt nicht die App-Sprache.

## Umfang

Bookshelf unterstützt de-DE, en-US, es-ES, fr-FR, it-IT und pt-BR.
Der fachliche Umfang bleibt Stufe 1. Es gibt keine Fotos, Projektstatus,
Checklisten oder PDF-Ausgabe. Der Markenname, die technischen Kennungen,
Datenpfade und SQLite-Daten bleiben erhalten.
Auf Nutzerauftrag vom 29. Oktober 2026 kommen JSON-Export und -Import hinzu.
Beide Aktionen und ihre Rückmeldungen sind in allen sechs Sprachen verfügbar.

Die Sprachwahl wird beim nächsten App-Start angewandt. Alle eigenen sichtbaren
Meldungen und Text-Kontextmenüs verwenden dieselbe Sprache. Die englische
Basis ist der Rückfall für nicht unterstützte Sprachen. Store-Texte und
synthetische Beispieldaten liegen je Sprache im Projekt.

## Voraussetzungen

`--mode startup --locales en-US` beim GUI-Helfer prüft den englischen Erststart
ohne Sprachdatei auf einem simulierten deutschen System.

Eclipse Temurin JDK 21.0.13+9, Python 3 und ein ausführbarer Linux-Xvfb-Server.
Der Server kann im PATH liegen oder durch BOOKSHELF_XVFB auf eine lokal
entpackte ausführbare Datei zeigen. Notwendige Systembibliotheken müssen
vorhanden sein. Die Aufnahmen benötigen keinen Zugriff auf DISPLAY=:0.
ImageMagick wird nur für das erneute Ableiten der Icons aus der SVG benötigt.

## Prüfen und aufnehmen

```bash
./mvnw spotless:apply
./mvnw verify dependency:build-classpath \
  -Dmdep.outputFile=target/gui-classpath.txt -Dmdep.includeScope=test
python3 scripts/store_assets/run_gui.py --output target/gui-international
```

`JAVA_HOME` und `PATH` müssen auf Temurin 21.0.13+9 zeigen. Das Skript sucht ersatzweise
unter `$HOME/.local/share/buecherregal/jdk-21.0.13+9`. Mit `--locales fr-FR` lässt sich eine Sprache gezielt
prüfen. `--mode capture` oder `--mode verify` begrenzen einen Entwicklungsdurchlauf.
Eine endgültige Lieferung verlangt `all` und alle sechs Sprachen.

Der zusätzliche Import-/Export-Ablauf bedient native Dateidialoge und benötigt
`xdotool` und `xsel`:

```bash
python3 scripts/store_assets/run_gui.py --output target/gui-transfer \
  --locales de-DE --mode transfer
```

Er prüft Export trotz Suchfilter, Abbrechen, ungültige Dateien, sichere
Importbestätigung, erhaltene IDs, Unicode, identische Exemplare, wiederholte
Importe und eine reale SQLite-Sperre mit erfolgreichem Wiederholungsversuch.

Der Helfer startet einen eigenen Xvfb-Server mit 1920 × 1080 Pixeln, deaktiviert
TCP und beendet ausschließlich den selbst gestarteten Prozess. Jede Sprache
bekommt eigene synthetische Daten unter `target/gui-work/`. Diese Verzeichnisse
werden nach dem Durchlauf entfernt. Vorhandene Datenbanken werden nicht ersetzt.

`GuiJourney` startet die produktive `BooksApplication` in einem neuen JVM-Prozess.
JavaFX Robot bedient sie mit echten Maus- und Tastaturereignissen. Der Bildschirm
wird unverändert als PNG aufgenommen. Es gibt keine nachgebaute Screenshot-UI,
keine überlagerten Werbetexte und keine privaten Bücherdaten.

Pro Sprache entstehen vier Vorschau-Bilder:

1. Übersicht mit Titeln, Autoren, Aufbewahrungsorten und ausgewählter Notiz.
2. Bearbeitungsdialog eines vorhandenen Buches.
3. Suchergebnis für einen passenden lokalen Begriff.
4. Sprachwahl mit allen unterstützten Sprachen.

Zusätzlich läuft je Sprache ein Prüfablauf mit Pflichtfeldhinweis, Unicode,
langer Notiz, Abbrechen und Weiterbearbeiten, echtem SQLite-Schreibfehler,
erneutem Speichern, abgebrochenem Löschen, fehlgeschlagenem Einstellungszugriff
und erneutem Speichern der Sprache. Kleine Fenster werden mitgeprüft.
Ein weiterer JVM-Prozess bestätigt die geänderte Sprache und die erhaltenen
Buchinhalte. Die Ausgangssystemsprache Japanisch weist nach, dass eine
explizite Einstellung tatsächlich Vorrang erhält.

## Herkunft und Lieferung

Nach den Aufnahmen alle Bilder visuell prüfen. `capture-manifest.json` enthält
Plattform, Aufnahmemethode, Bildschirmgröße, UTC-Zeit, Ausgangscommit sowie
SHA-256-Werte der Runtime-, Test-, Werkzeug- und Asset-Quellen.
Dokumentation kann danach ergänzt werden. Laufzeitcode und Aufnahmewerkzeuge
dürfen sich bis zur Verpackung nicht mehr ändern.

Nach erfolgreicher Prüfung und lokalem Commit:

```bash
python3 scripts/store_assets/assemble.py target/gui-international \
  build/releases/international/buecherregal-linux-preview-YYYYMMDD-HHMM
```

Der Zielordner muss neu sein. Es wird ausschließlich ein Ordner erzeugt. Die Verpackung
prüft Quellhashes, sauberen Git-Stand, alle 18 erfolgreichen GUI-Prozesse,
Bildanzahl, PNG-Format und Abmessungen. Sie liefert sechs Text- und Bildpakete,
Branding, Prüfnachweise, einen Herkunftsnachweis mit dem tatsächlichen lokalen
Commit und Dateiprüfsummen. Zusätzlich enthält `Privacy` die unveränderte
englische `PRIVACY.md` und `privacy-url.txt`. Der Browseraufruf und die
lokalisierte Fehleranzeige mit kopierbarer URL werden in `InfoMenuUiTest`
geprüft. Frühere Vorschauordner behalten ihren damaligen Quellstand.

## Branding

Die eigene SVG zeigt drei Bücher im Regal. Die Formen sind originale Vektoren
und enthalten weder Stockbilder noch fremde Schriftarten. Die App verwendet
das 256-Pixel-PNG als Fenstericon. Die Linux-App-Image-Paketierung nutzt dasselbe
Icon. Die 1024-Pixel-Quelle, das 300-Pixel-Store-Icon und die Windows-ICO-Datei
liegen unter `assets/branding`. Das PowerShell-Skript übergibt das ICO ausdrücklich an jpackage.
Die tatsächliche Darstellung in einem Windows-Paket ist noch nicht geprüft.

```bash
bash scripts/generate_icons.sh
```

## Offene Windows-Schritte

Die Lieferung ist ausdrücklich eine Linux-Vorschau. Das Erzeugen und Ausführen
des Windows-Pakets, Installations- und Updateprüfung, endgültige Windows-Bilder,
Partner-Center-Identität und Store-Einreichung gehören zum Folgeauftrag.
Ein erfolgreicher Linux-Durchlauf ersetzt diese Abnahme nicht.
