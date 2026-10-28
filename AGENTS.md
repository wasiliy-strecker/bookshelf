# BücherRegal

Eigenständige Java-Desktop-App, Stufe 1 des bestätigten Hobby-Portfolios.
Die übergeordneten Workspace-Regeln gelten ergänzend.

## Grenzen

Ein Maven-Modul mit Java 21, JavaFX und SQLite. Die Pakete `domain`,
`application`, `persistence` und `ui` trennen Modell, Anwendungslogik,
Datenzugriff und Oberfläche. Keine Runtime-Abhängigkeit auf andere Apps.
Fotos, Status, Checklisten und PDF-Export gehören zu den späteren Apps.

## Befehle

Alle Befehle aus diesem App-Verzeichnis mit vollständigem JDK 21 ausführen.

```bash
./mvnw spotless:apply
./mvnw verify
./mvnw javafx:run
./scripts/run.sh
./scripts/package.sh app-image
```

`scripts/run.sh` und `scripts/package.sh` finden auch ein vorhandenes JDK unter
`$HOME/.local/jdk-21`, wenn `JAVA_HOME` nicht gesetzt ist. Für direkte Maven-
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

Sechs Sprachen: de-DE, en-US, es-ES, fr-FR, it-IT und pt-BR. Alle eigenen
sichtbaren Texte liegen in UTF-8-ResourceBundles. Der Markenname bleibt
BücherRegal. Keine übersetzten Speicherfehler im Repository erzeugen.
`StorageException.Code` wird erst in der UI übersetzt. Neue Schlüssel in allen
sechs Dateien ergänzen. Sprachwechsel dürfen Buchdaten nicht verändern.

GUI-Aufnahmen verwenden `scripts/store_assets/run_gui.py`, die echte
`BooksApplication` und einen eigenen Xvfb-Prozess. Keine Nutzerfenster bedienen.
Die Datenverzeichnisse müssen ausdrücklich als synthetisch markiert sein.
Vor Aufnahmen `test-compile` und den Test-Classpath aktualisieren. Nach finalen
Quelländerungen Aufnahmen neu erstellen, damit die Prüfsummen übereinstimmen.
Die Anleitung steht in `docs/internationalization.md`.

Lieferpakete sind Linux-Vorschauen. Keine Windows-Abnahme daraus ableiten.
`assemble.py` verlangt den sauberen lokalen Commit des geprüften Quellstands.
Generierte Lieferungen gehören unter `build/` und nicht in Git. Roadmap unter
`/tmp/app-factory-cpp-roadmap.lock` frisch lesen und gezielt aktualisieren.
