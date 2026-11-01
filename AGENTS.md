# Bookshelf

Eigenständige Java-Desktop-App, Stufe 1 des bestätigten Hobby-Portfolios.
Die übergeordneten Workspace-Regeln gelten ergänzend.

## Grenzen

Ein Maven-Modul mit Java 21, JavaFX und SQLite. Die Pakete `domain`,
`application`, `persistence` und `ui` trennen Modell, Anwendungslogik,
Datenzugriff und Oberfläche. Keine Runtime-Abhängigkeit auf andere Apps.
Fotos, Status, Checklisten und PDF-Export gehören zu den späteren Apps.
Auf ausdrücklichen Nutzerauftrag vom 29. Oktober 2026 ersetzt ein JSON-Export
und -Import der Büchersammlung die Schaltfläche „Neu laden“. Der Import ergänzt
fehlende Exemplare atomar, erhält bestehende IDs und löscht keine Bücher.
Diese Erweiterung gilt ausschließlich für Bookshelf.

## Befehle

Alle Befehle aus diesem App-Verzeichnis mit Eclipse Temurin JDK 21.0.13+9 ausführen.

```bash
./mvnw spotless:apply
./mvnw verify
./mvnw javafx:run
./scripts/run.sh
./scripts/package.sh app-image
```

`scripts/run.sh` und `scripts/package.sh` finden auch ein vorhandenes JDK unter
`$HOME/.local/share/buecherregal/jdk-21.0.13+9`, wenn `JAVA_HOME` nicht gesetzt ist. Für direkte Maven-
Befehle `JAVA_HOME` und `PATH` entsprechend setzen.

Für Windows den Wrapper `mvnw.cmd` verwenden. Die native Paketierung wird mit
`./scripts/package.ps1 -PackageType exe` auf Windows und
`./scripts/package.sh dmg` auf macOS vorbereitet. Diese beiden nativen Wege
sind in dieser Linux-Umgebung noch nicht ausgeführt worden.

## Änderungsregeln

Verhalten mit fokussierten Tests oder einem dokumentierten GUI-Ablauf prüfen.
Für SQLite-Tests `@TempDir` verwenden. Für Oberflächentests ein eigenes
`--data-dir` und ausschließlich synthetische Bücher verwenden.
Nach Änderungen Formatter, `verify` und passende manuelle Prüfungen ausführen.
Vor einem Commit müssen die erforderlichen Prüfungen bestehen.

Datenbankzugriffe bleiben außerhalb des JavaFX-Threads. Ein Dialog darf erst
nach erfolgreichem Speichern schließen. Abbrechen, Escape und Fensterschließen
müssen denselben Schutz für ungespeicherte Änderungen verwenden.

Paketierung ersetzt `verify` nicht. Das App-Image zusätzlich sichtbar starten.
Ein erfolgreicher `--version`-Aufruf allein belegt keine funktionierende GUI.
Keine generierten Pakete, Datenbanken oder lokale Konfiguration committen.
Kein Push, Upload oder Store-Release ohne ausdrücklichen Auftrag.

## Internationalisierung und Vorschau

Die erste Veröffentlichung erfolgt auf ausdrücklichen Nutzerauftrag vom
31. Oktober 2026 auf Deutsch. Primärsprache im Store: Deutsch (de-DE).
Beschreibung, Bildtexte und Versionshinweise für die erste Veröffentlichung
sind deutsch. Das betrifft die Release- und Store-Sprache. Die App startet ohne
gespeicherte Sprachwahl auf Englisch, unabhängig von der Systemsprache.
Vorhandene Sprachwahlen einschließlich ausdrücklich gewähltem „Automatisch“
bleiben erhalten. Alle vorhandenen UI-Übersetzungen bleiben auswählbar.
Der App- und Store-Name bleibt **Bookshelf**. Weitere Store-Sprachen
werden erst später auf Nutzerauftrag hinzugefügt. Bereits vorbereitete
Übersetzungen sind keine Freigabe für ein mehrsprachiges erstes Store-Angebot.

Vorhandene Ressourcen: de-DE, en-US, es-ES, fr-FR, it-IT und pt-BR. Alle eigenen
sichtbaren Texte liegen in UTF-8-ResourceBundles. Auf ausdrücklichen Nutzerauftrag
vom 1. November 2026 heißen App und Store-Produkt in allen Sprachen **Bookshelf**,
passend zum englischen GitHub-Repository `bookshelf`. Das gilt auch für deutsche
Oberflächen, Launcher, Fenstertitel, Installer, Store-Einträge und Empfehlungen
im Gespräch. Die bisherige Vorgabe, den Markennamen BücherRegal beizubehalten,
ist aufgehoben. Bestehende technische Kennungen und Datenpfade werden durch
diese Namensregel nicht geändert.
Keine übersetzten Speicherfehler im Repository erzeugen.
`StorageException.Code` wird erst in der UI übersetzt. Neue Schlüssel in allen
sechs Dateien ergänzen. Sprachwechsel dürfen Buchdaten nicht verändern.

GUI-Aufnahmen verwenden `scripts/store_assets/run_gui.py`, die echte
`BooksApplication` und einen eigenen Xvfb-Prozess. Keine Nutzerfenster bedienen.
Die Datenverzeichnisse müssen ausdrücklich als synthetisch markiert sein.
Vor Aufnahmen `test-compile` und den Test-Classpath aktualisieren. Nach finalen
Quelländerungen Aufnahmen neu erstellen, damit die Prüfsummen übereinstimmen.
Die Anleitung steht in `docs/internationalization.md`.

Der Datenschutzdialog und die exportierten Store-Datenschutztexte verwenden
dieselben ResourceBundles. Die Vorschau-Lieferung ist ein Ordner ohne ZIP.
Microsoft Store: MSIX. Kostenloser GitHub-Download: EXE. Beide Wege verwenden
denselben Anwendungscode. Native Windows-Paketierung und Abnahme bleiben separat.

`scripts/package_msix.ps1` baut auf Windows ein unsigniertes MSIX ohne Tests
oder Launcher-Start. Echte Werte aus Partner Center in der ignorierten Datei
`packaging/windows/store.local.json` sind erforderlich. Es wird nichts installiert
oder hochgeladen. Anleitung: `docs/microsoft-store-release.md`.
`scripts/prepare_store_release.py` erstellt eine interne Übergabe aus dem
committeten Stand ohne Tests. Sie ist keine geprüfte Vorschau von `assemble.py`
und keine Store-Freigabe. Unversionierte persönliche Dateien bleiben außen vor.

Lieferpakete sind Linux-Vorschauen. Keine Windows-Abnahme daraus ableiten.
`assemble.py` verlangt den sauberen lokalen Commit des geprüften Quellstands.
Generierte Lieferungen gehören unter `build/` und nicht in Git. Roadmap unter
`/tmp/app-factory-cpp-roadmap.lock` frisch lesen und gezielt aktualisieren.

Am 1. November 2026 hat der Nutzer den öffentlichen Namen auf **Bookshelf**
gekürzt. Dies ersetzt die Namensentscheidung vom 31. Oktober 2026. Der damals
reservierte Store-Name war **Bookshelf by Appfabrik**. Die lokalen Vorlagen
verwenden jetzt **Bookshelf**. Online-Store-Einträge und Namensreservierungen
wurden dadurch nicht geändert.
Die Mac-Erstveröffentlichung wird für Apple Silicon und Intel vorbereitet.
Native Mac-Abnahme, Signierung und Upload erfolgen separat auf macOS.
