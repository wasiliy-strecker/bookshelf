# BücherRegal

Eine kleine Desktop-App für deine eigene Büchersammlung. Erfasse Titel, Autor,
Aufbewahrungsort und eine Notiz. Finde Bücher über alle vier Felder wieder.
Die Sammlung bleibt lokal auf deinem Computer und ist nach dem Neustart erhalten.

![BücherRegal mit synthetischen Beispieldaten](docs/screenshots/buecherregal.png)

BücherRegal ist Stufe 1 des Java-Hobby-Portfolios. Die Reihe führt über GarnBox
(Foto), StrickProjekt (Status) und NähPlan (Checkliste) bis zur fünften Stufe
KostümPlan (PDF-Export). Diese weiteren Apps bleiben eigenständige Projekte.

## Starten

Für die Entwicklung brauchst du ein **vollständiges Eclipse Temurin JDK 21.0.13+9**. `JAVA_HOME` muss auf dieses JDK zeigen. Maven wird über den
beiliegenden Wrapper bezogen. Beim ersten Build ist Internet für die
Abhängigkeiten erforderlich. Die App selbst arbeitet offline.

Die Build-Version ist app-lokal festgelegt. Maven lehnt eine abweichende
JDK-Version oder Distribution ab. Das JDK liegt auf diesem Entwicklungsrechner
unter `$HOME/.local/share/buecherregal/jdk-21.0.13+9`. Andere Apps verwenden
weiterhin ihre bisherigen Java-Installationen.

Linux und macOS, aus dem Projektverzeichnis:

```bash
export JAVA_HOME="$HOME/.local/share/buecherregal/jdk-21.0.13+9"
export PATH="$JAVA_HOME/bin:$PATH"
./mvnw javafx:run
```

Windows PowerShell:

```powershell
.\mvnw.cmd javafx:run
```

Unter Linux gibt es außerdem `./scripts/run.sh`. Der Helfer verwendet bei
fehlendem `JAVA_HOME` ein vorhandenes vollständiges JDK unter
`$HOME/.local/share/buecherregal/jdk-21.0.13+9`. Er installiert nichts und ändert keine globale Einstellung.

Das lokal erzeugte App-Image startet ohne separat installiertes Java:

```bash
./target/app-image/BuecherRegal/bin/BuecherRegal
```

## Bedienung

„Buch hinzufügen“ öffnet den Eingabedialog. Nur der Titel ist erforderlich.
Doppelklick oder Enter auf einer Tabellenzeile öffnet die Bearbeitung.
Löschen verlangt eine Bestätigung. Ungespeicherte Änderungen werden vor dem
Verwerfen abgefragt. Nach einem Speicherfehler bleiben deine Eingaben erhalten.

| Taste | Aktion |
| --- | --- |
| Strg+N / Cmd+N | Buch hinzufügen |
| Strg+F / Cmd+F | Suche fokussieren und Suchtext auswählen |
| Strg+S / Cmd+S | Dialog speichern |
| Escape | Dialog abbrechen, gegebenenfalls mit Rückfrage |
| Enter im Notizfeld | Neue Zeile |
| Entf in der Tabelle | Ausgewähltes Buch nach Bestätigung löschen |

## Sammlung exportieren und importieren

„Exportieren“ speichert die gesamte gespeicherte Sammlung als JSON-Datei.
Titel, Autor, Aufbewahrungsort und vollständige Notizen sind enthalten.
Eine aktive Suche schränkt den Export nicht ein. Vor dem Ersetzen einer
vorhandenen Exportdatei wird eine Bestätigung angezeigt.

„Importieren“ öffnet eine solche Datei und fragt vor der Übernahme nach.
Vorhandene Bücher bleiben erhalten. Identische Exemplare werden mit ihrer
Anzahl verglichen. Ein wiederholter Import derselben Datei fügt deshalb keine
weiteren Kopien hinzu. Unterschiedliche Notizen ergeben unterschiedliche Einträge.
Nach dem Import zeigt die App alle Bücher und meldet die Anzahl der hinzugefügten
und bereits vorhandenen Exemplare.

Die Datei wird vollständig geprüft, bevor Bücher übernommen werden. Bei einem
Fehler wird der gesamte Import zurückgenommen. Eine Datei darf höchstens
10.000 Bücher und 10 MiB enthalten. Die Datei funktioniert unabhängig von der
gewählten App-Sprache. Die persönliche Sprachwahl ist nicht Teil der Sammlung.
Die Schaltfläche „Neu laden“ entfällt. Nach Änderungen aktualisiert sich die
Liste weiterhin automatisch.

Neue Exportdateien benötigen Hardlinks, das bestätigte Ersetzen benötigt einen
atomaren Dateiaustausch. FAT32 und exFAT unterstützen den sicheren direkten
Export nicht. Exportiere zuerst in einen geeigneten lokalen Ordner, unter
Windows etwa auf NTFS. Kopiere danach die fertige JSON-Datei auf den USB-Datenträger.
Auch ein Netzwerk- oder Cloud-Dateisystem kann diese Funktionen einschränken.
Vorhandene Exporte werden bei einem Fehler nicht durch unvollständige Dateien ersetzt.

## Datenschutz in der App

Unter „Info“ → „Datenschutzerklärung für BücherRegal“ ist die vollständige
Erklärung in der gewählten App-Sprache enthalten. Der Dialog funktioniert
offline. Du kannst scrollen, mit Strg+A und Strg+C beziehungsweise Cmd+A und
Cmd+C den Text kopieren und mit Escape oder „Schließen“ zurückkehren.
Die Erklärung beschreibt die lokale Speicherung, unverschlüsselte JSON-Dateien,
eigene Sicherungskopien und freiwillige Supportanfragen. Sie sendet keine Daten
und verändert weder Sammlung noch Spracheinstellung.

Die identischen Texte für die Store-Einreichung lassen sich in einen neuen
Ordner exportieren, dessen übergeordnetes Verzeichnis bereits existiert:

```bash
./scripts/export_privacy.sh target/privacy-texts
```

Windows nach `mvnw.cmd test-compile`:

```powershell
& "$env:JAVA_HOME\bin\java.exe" -cp 'target/classes;target/test-classes' `
  de.appfabrik.buecherregal.i18n.PrivacyTextExport target/privacy-texts
```

Es entstehen sechs UTF-8-Textdateien aus denselben Ressourcen wie der App-Dialog.
Ein vorhandener Zielordner wird nicht überschrieben. Der Text muss zusätzlich
im Partner Center hinterlegt werden. Eine erfolgte Einreichung wird damit
nicht behauptet. Details stehen in [packaging.md](docs/packaging.md).

## Sprachen

Die Oberfläche unterstützt Deutsch, Englisch, Spanisch, Französisch,
Italienisch und brasilianisches Portugiesisch. Beim ersten Start folgt sie
für diese Sprachen der Systemsprache. Für andere Systemsprachen wird Englisch
verwendet. Regionale Varianten wie de-AT und pt-PT werden der vorhandenen
deutschen beziehungsweise brasilianisch-portugiesischen Übersetzung zugeordnet.

Über „Sprache“ wählst du eine feste Sprache oder wieder „Automatisch“.
Die Auswahl wird nach dem nächsten App-Start übernommen. Die App startet nicht
selbständig neu. Die Sammlung und offene Buchentwürfe bleiben erhalten.
Der Markenname BücherRegal bleibt in allen Sprachen gleich.

Die Einstellung liegt in `language.txt` neben der Datenbank. Ein ungültiger
Sprachwert fällt auf Englisch zurück. Bei einem Lesefehler nutzt die App die
Systemsprache mit einem sichtbaren Hinweis. Speichern erfolgt atomar.
Ein fehlgeschlagener Schreibvorgang lässt die Auswahl im Dialog stehen.

## Daten und Tests

Die Datei `buecherregal.db` liegt unter:

| System | Verzeichnis |
| --- | --- |
| Windows | `%LOCALAPPDATA%\BuecherRegal` |
| macOS | `~/Library/Application Support/BuecherRegal` |
| Linux | `$XDG_DATA_HOME/buecherregal`, sonst `~/.local/share/buecherregal` |

Zum manuellen Sichern die App schließen und die Datenbankdatei kopieren.
Für isolierte Tests kann ein eigenes Verzeichnis angegeben werden:

```bash
./mvnw javafx:run '-Djavafx.args=--data-dir=/tmp/buecherregal-test'
```

Der paketierte Launcher akzeptiert direkt `--data-dir=/absoluter/testpfad`.
Ein abweichender Testpfad verändert nicht den Standardspeicherort.

```bash
./mvnw spotless:apply
./mvnw verify
```

`verify` führt Compiler-Lint mit Warnungen als Fehlern, Maven Enforcer,
JUnit-Tests, Formatprüfung und SpotBugs aus. Die Tests verwenden temporäre
SQLite-Dateien. Es werden keine privaten Sammlungen geöffnet.

## Vorschau-Assets für den Microsoft Store

[Internationalisierung und Vorschau-Erzeugung](docs/internationalization.md)
beschreibt reproduzierbare Aufnahmen der echten Linux-Oberfläche in einem
isolierten virtuellen Bildschirm. Pro Sprache entstehen vier Bilder mit
synthetischen Büchern, insgesamt 24 Bilder in 1920 × 1080 Pixeln.

Die Klartexte in `store-listings/` sind direkt kopierbar. Jede Sprache hat eine
Beschreibung, Kurzbeschreibung, Funktionsliste und vier Bildunterschriften.
Das eigene [App-Icon](assets/branding/icon.svg) liegt außerdem als PNG und
Windows-ICO vor. Das Store-Icon hat 300 × 300 Pixel.

Diese Lieferung ist eine Linux-Vorschau. Windows-Paketierung, native Abnahme,
endgültige Windows-Store-Bilder und Einreichung bleiben separate Schritte.
Generierte Lieferungen liegen unter dem ignorierten Verzeichnis `build/`.

## Architektur und Auslieferung

Temurin 21.0.13+9, JavaFX 21.0.11 und SQLite JDBC 3.53.4.0 in einem Maven-Modul.
Die [Architekturerklärung](docs/architecture.md) erläutert den Ablauf
„Buch speichern“ und die dabei sichtbaren Java-Kenntnisse.

Die [Paketierungsanleitung](docs/packaging.md) beschreibt App-Images mit
eingebetteter Laufzeit, Windows-EXE und macOS-DMG. Für den Microsoft Store ist MSIX vorgesehen,
für den kostenlosen GitHub-Download eine EXE aus demselben Anwendungscode.
Die MSIX-Paketierung und die Freigabe beider Windows-Pakete folgen separat. Die vorbereiteten
GitHub-Workflows veröffentlichen keine Releases.

Der [Prüfbericht](docs/verification.md) trennt ausgeführte Prüfungen von offenen
Plattformtests. Der aktuelle App-Stand ist unter Linux geprüft. Ein älterer CI-Lauf
prüfte 33 Tests auf Windows und macOS. Native Abnahmen des aktuellen Stands
und Store-Abnahmen bleiben offen. Ein Installer ist keine geprüfte Store-Version.

Hinweise zu Fremdkomponenten stehen in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
