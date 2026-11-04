# Bookshelf auf dem Mac abschließen

Diese Lieferung bereitet Version **1.0**, Apple-Build **2**, für Apple Silicon
und Intel vor. Sie enthält noch kein nativ geprüftes oder signiertes Mac-Paket.
Die vollständige Arbeit erfolgt im mitgelieferten Ordner `Source`.
`MacInputs` enthält bereits geprüfte Java-Klassen und originale Mac-Abhängigkeiten.

## Voraussetzungen und Grenzen

Benötigt werden ein Mac, Xcode mit Command Line Tools, Python **3.12 oder neuer**,
Eclipse Temurin **21.0.13+9** für die Prozessorfamilie des ausführenden Macs und
Zugriff auf die eigene Apple-Entwicklermitgliedschaft. `JAVA_HOME` muss auf das
`Contents/Home` dieses JDK zeigen. Ein passendes JDK-Archiv liegt in
`MacInputs/dependencies`. Es kann lokal entpackt werden. Keine systemweite
Java-Installation ist erforderlich.

Der native Launcher wird als Universal-Binary kompiliert. Zwei getrennte,
mit `jlink` aus den jeweiligen JMODs erzeugte Laufzeiten werden eingebettet.
Der aktive Launcher lädt die zur Prozessarchitektur passende JLI-Bibliothek
im selben Prozess. Auch JavaFX und SQLite liegen für beide Architekturen vor.
Auf Apple Silicon läuft der ARM64-Teil ohne Rosetta. Ein Intel-Ausführungstest
unter Rosetta ist möglich, ersetzt aber keine Abnahme auf echter Intel-Hardware.

Die Mac-Schritte sind hier noch nicht ausgeführt. Insbesondere Cocoa-Launcher,
JIT-Berechtigung, native JavaFX-Dateidialoge, Universal-Bundle-Annahme durch Apple
und Signierung benötigen den tatsächlichen Mac-Test. Der Helfer schaltet
Library Validation nicht ab und ergänzt weder Netzwerkrechte noch pauschale
Dateisystemausnahmen. Bei einem nativen Fehler zuerst dessen Ursache ermitteln.
Ein signiertes Paket allein ist kein Beleg für erfolgreiche GUI- oder Store-Abnahme.

Die Bundle-Prüfung kontrolliert auch die Kennungen eingebetteter Laufzeiten und
für alle Benutzer lesbare Dateien sowie betretbare Verzeichnisse. Der Intel-Pfad
bleibt `Runtime-x86_64.bundle`; seine Bundle-ID endet auf `runtime.x86-64`.
Das eingebettete Provisioning-Profil erhält vor der Signierung die Dateirechte
`0644`. Das lokale Originalprofil behält seine privaten Dateirechte.

## 1. Quellen und Eingaben prüfen

Den gesamten Übergabeordner kopieren. Im Terminal in dessen `Source` wechseln.

```bash
python3 scripts/macos_release.py check --inputs ../MacInputs
```

Der Prüfschritt kontrolliert den Quellstand, sämtliche Eingabeprüfsummen,
Store-Feldlängen und die Architektur der Originalbibliotheken. Änderungen an
App-Code, Bundle-Konfiguration oder Releasehelfer erfordern neue Eingaben.
`Source` enthält weder private Sammlungen noch Zertifikate oder Tokens.

Für eine erneute Vorbereitung aus einem geänderten Repository zuerst
`./mvnw verify` ausführen. Danach einen neuen Ausgabeordner verwenden:

```bash
python3 scripts/macos_release.py prepare --output build/mac-inputs-neu
```

## 2. Universal-App bauen

Im Beispiel ist `JAVA_HOME` bereits auf das passende entpackte Temurin-JDK gesetzt.

```bash
python3 scripts/macos_release.py build --inputs ../MacInputs --output ../MacBuild-1.0-2
python3 scripts/macos_release.py inspect --app "../MacBuild-1.0-2/Bookshelf.app"
```

Der Helfer erzeugt neue Ordner und überschreibt keine frühere Lieferung.
Das Ergebnis enthält beide Architekturpfade. Die Bundles werden noch nicht
gestartet oder hochgeladen. Der Mac-Store-Launcher verweigert einen Start ohne
aktiven App-Container. Für die folgende native Abnahme ist deshalb zunächst
eine Entwicklungssignatur mit Sandbox erforderlich.

## 3. Entwicklungssignatur und native Abnahme

`packaging/macos/signing.example.json` nach `packaging/macos/development.local.json`
kopieren. Ausschließlich lokal ausfüllen:

| Feld | Inhalt |
| --- | --- |
| teamId | Eigene zehnstellige Apple-Team-ID |
| appStoreConnectBundleId | Die im bestehenden App-Store-Connect-Eintrag gewählte Bundle-ID |
| applicationIdentity | Vollständiger Name des Apple-Development-Zertifikats im Schlüsselbund |
| installerIdentity | Für diesen Entwicklungsschritt leer lassen |
| provisioningProfile | Absoluter Pfad zum passenden Mac-Entwicklungsprofil |

Das Profil muss die konkrete App-ID abdecken und für den Test-Mac gültig sein.
Die bestehende Kennung lautet `de.appfabrik.buecherregal`. Bei einem abweichenden
Store-Eintrag nicht eigenmächtig Paket-ID oder Datenpfade ändern.

```bash
python3 scripts/macos_release.py sign --app "../MacBuild-1.0-2/Bookshelf.app" --config packaging/macos/development.local.json --mode development
open "../MacBuild-1.0-2/Bookshelf.app"
```

Für die Abnahme einen separaten macOS-Testbenutzer mit synthetischen Büchern
verwenden. Die App erhält ihren Datenordner über Foundation innerhalb ihres
App-Containers. Eine bestehende ungesandboxte Sammlung wird nicht importiert,
verschoben oder gelöscht. Der Übergang erfolgt später nur durch bewusstes
JSON-Exportieren und Importieren.

In beiden Architekturpfaden prüfen:

| Ablauf | Erwartung |
| --- | --- |
| Start ohne Internet und ohne globales Java | Oberfläche erscheint, eingebettete Laufzeit wird verwendet |
| Buch anlegen, bearbeiten, suchen, löschen | Texte, Umlaute und Notizen korrekt |
| App schließen und erneut öffnen | Sammlung und Sprache erhalten |
| Exportordner wählen, Dateinamen bestätigen | Vollständige JSON-Datei im freigegebenen Ordner |
| Beide Exportdialoge abbrechen | Kein Export und keine Änderung an bestehenden Dateien |
| Vorhandenen Export bestätigen oder ablehnen | Ausschließlich bestätigter Ersatz, vorhandene Datei bei Fehler erhalten |
| JSON importieren und denselben Import wiederholen | Bestehende Bücher erhalten, keine zusätzlichen identischen Exemplare |
| Nicht beschreibbaren Exportordner wählen | Verständliche Fehlermeldung, keine unvollständige Lieferung |
| Info öffnen, Sprache wechseln, kleine Fenster | Vollständiger Datenschutz, lesbare Texte, bedienbare Schaltflächen |

Auf Apple Silicon lässt sich der Intel-Pfad nach Installation von Rosetta
gezielt ausführen. Die App zuvor schließen. Dieser Befehl verändert keine Daten:

```bash
arch -x86_64 "../MacBuild-1.0-2/Bookshelf.app/Contents/MacOS/Bookshelf"
```

Sandbox-Verstöße in der macOS-Konsole untersuchen. Eine fehlgeschlagene
Dateiberechtigung nicht durch allgemeine Festplatten- oder Netzwerkrechte umgehen.
Die normalen Installationen außerhalb des Stores behalten ihren bisherigen
Dateiauswahldialog. Nur der Store-Build fordert den Zielordner an.

## 4. Endgültige Screenshots

Vier Ansichten auf dem Mac mit denselben erfundenen Büchern aufnehmen:
Sammlung, Bearbeitung mit Notiz, Suche, JSON-Export mit Dateiname.
Die Inhalte stehen unter `assets/store_demo/de-DE.properties`.
Zum Importieren liegt `assets/store_demo/bookshelf-de-DE.json` bei.

Die realen Mac-Aufnahmen als PNG in **2560 × 1600** unter
`Screenshots-Final-Mac/de-DE` ablegen. Fenster bei der Aufnahme vollständig
sichtbar halten. Keine echten Bücher, Dateipfade mit privaten Informationen
oder offenen Nutzerfenster aufnehmen. Die Linux-Dateien sind nur Bildentwürfe.
Die Bildtexte aus `Store/de-DE/captions.txt` können verwendet werden.

## 5. Store-Signatur und Paket

Nach erfolgreicher Abnahme eine weitere lokale Konfiguration
`packaging/macos/store.local.json` anlegen. Darin das Apple-Distribution-
beziehungsweise „3rd Party Mac Developer Application“-Zertifikat und das
passende Mac-App-Store-Profil verwenden. Für das Paket das Zertifikat
„3rd Party Mac Developer Installer“ beziehungsweise „Mac Installer Distribution“
eintragen. Developer-ID-Zertifikate für direkte Downloads sind hierfür falsch.
Private Schlüssel und Profile bleiben auf dem Mac.

Die Entwicklungssignatur wird jetzt ausdrücklich durch die Store-Signatur ersetzt.
Das fertige `.pkg` wird unter einem neuen Namen abgelegt und nie überschrieben.

```bash
python3 scripts/macos_release.py sign --app "../MacBuild-1.0-2/Bookshelf.app" --config packaging/macos/store.local.json --mode store
python3 scripts/macos_release.py inspect --app "../MacBuild-1.0-2/Bookshelf.app" --signed
```

Die Datei `packaging/macos/acceptance.example.json` nach
`packaging/macos/acceptance.local.json` kopieren. Nur tatsächlich bestandene
Prüfungen auf `true` setzen. Bei `x86_64Method` `native` oder `rosetta` angeben.
Das Abnahmeprotokoll beschreibt die Umgebung, Fehlerbefunde und das Ergebnis.

```bash
python3 scripts/macos_release.py package --app "../MacBuild-1.0-2/Bookshelf.app" --config packaging/macos/store.local.json --acceptance packaging/macos/acceptance.local.json --output "../Bookshelf-1.0-build-2-universal.pkg"
```

Das Paket anschließend in Transporter öffnen und validieren. Nach erfolgreichem
Upload und Apples Verarbeitung erscheint der Build in App Store Connect.
Kein Skript dieser Vorbereitung lädt selbst etwas hoch.

## 6. Einreichung abschließen

Die separate Feldanleitung abarbeiten. Öffentliche Datenschutzadresse und
vollständige Laufzeitquellen bereitstellen. Die Mac-Metadaten der beiden
Temurin-Archive liegen bei den vorbereiteten Quellen. Die genaue Inventur,
erforderliche Privacy-Manifeste beziehungsweise API-Gründe und die Export-
Compliance des tatsächlich gebauten Bundles vor Einreichung prüfen.
Die App selbst übermittelt keine Sammlung und verwendet keine Analytics-SDKs.

Nach der nativen Abnahme, erfolgreicher Transporter-Prüfung und vollständigen
Metadaten in App Store Connect den verarbeiteten Build auswählen und zur Prüfung
hinzufügen. Apples App Review bleibt erforderlich.

## Technische Referenzen

[Apple: Universal-Binaries](https://developer.apple.com/documentation/apple-silicon/building-a-universal-macos-binary)

[Apple: App Sandbox](https://developer.apple.com/documentation/security/protecting-user-data-with-app-sandbox)

[OpenJDK: JLI-Schnittstelle](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/native/libjli/java.h)

[Oracle: Java-Paketierung und Store-Signierung](https://docs.oracle.com/en/java/javase/21/jpackage/support-application-features.html)

## Vorbereitung unter Linux erneut erzeugen

Diese Befehle betreffen die Vorbereitung, nicht die native Mac-Abnahme.
Sie benötigen das festgelegte JDK, Python 3.12, Xvfb, xdotool und xsel.
Ein vorhandenes Xvfb kann über `BOOKSHELF_XVFB` angegeben werden. Für jede
Wiederholung neue Ausgabeordner wählen. Eigene Sammlungen werden nicht verwendet.

```bash
./mvnw --batch-mode --no-transfer-progress spotless:check verify > target/mac-preparation-verify.log 2>&1
./mvnw --batch-mode --no-transfer-progress dependency:build-classpath -Dmdep.outputFile=target/gui-classpath.txt
python3 scripts/test_macos_release.py > target/mac-preparation-python-tests.log 2>&1
python3 scripts/store_assets/run_gui.py --output target/mac-gui-neu --mode all > target/mac-preparation-gui.log 2>&1
python3 scripts/store_assets/run_gui.py --output target/mac-transfer-neu --mode transfer --locales de-DE > target/mac-preparation-transfer.log 2>&1
python3 scripts/store_assets/run_gui.py --output target/mac-store-neu --mode store --width 2560 --height 1600 --locales de-DE > target/mac-preparation-store.log 2>&1
python3 scripts/macos_release.py prepare --output build/mac-inputs-neu > target/mac-preparation-inputs.log 2>&1
```

Nach erfolgreicher Prüfung den geprüften Quellstand lokal committen.
Dann den Ordner erzeugen. Die Übergabe gleicht Bilder, GUI-Nachweise,
Mac-Eingaben und aktuellen Quellstand über Prüfsummen ab.

```bash
python3 scripts/prepare_mac_handoff.py --inputs build/mac-inputs-neu --captures target/mac-store-neu --gui-verification target/mac-gui-neu --transfer-verification target/mac-transfer-neu --output build/releases/mac-handoff-neu
```

`SHA256SUMS.txt` im erzeugten Ordner erfasst jede gelieferte Datei.
Der Generator übernimmt ausschließlich versionierte Quellen. Persönliche
unversionierte Exporte und ignorierte Signierungskonfigurationen bleiben außerhalb.
