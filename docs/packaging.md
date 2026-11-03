# Paketierung und spätere Stores

## Voraussetzungen

Zuerst `./mvnw verify` beziehungsweise `.\mvnw.cmd verify` erfolgreich ausführen.
Paketierung kompiliert den aktuellen Stand, wiederholt jedoch nicht die Tests.
Ein vollständiges Eclipse Temurin JDK 21.0.13+9 mit `javac`, `jlink` und
`jpackage` ist erforderlich. Maven erzwingt Hersteller und vollständige
Laufzeitversion. Die beiden CI-Workflows verwenden dieselbe Version.
JavaFX-Bibliotheken und JDK müssen zur Zielarchitektur passen.

Unter Linux finden die Skripte bei fehlendem `JAVA_HOME` die app-lokale
Installation unter `$HOME/.local/share/buecherregal/jdk-21.0.13+9`.
Es wird keine globale Java-Installation verändert. Die offizielle
[Temurin-Lieferung](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.13%2B9)
stellt Binärdateien, Prüfsummen und zugehörigen Quellcode bereit.

Das App-Image enthält die App, SQLite JDBC und eine erzeugte Java-Laufzeit.
JavaFX wird als Modul in die Laufzeit eingebunden. Die eigene App bleibt ein
gewöhnliches Java-Projekt ohne `module-info.java`. Der Anwender benötigt kein
separates Java. Die vollständigen Runtime-Verzeichnisse müssen zusammenbleiben.
`legal/BUILD_JAVA_RELEASE.txt` enthält die unveränderten Herkunftsangaben des
Build-JDK. `legal/USER_GUIDE.md` enthält die Bedienungsanleitung mit Datenpfaden.

## Lokales Linux-App-Image

```bash
./scripts/package.sh app-image
'./target/app-image/Bookshelf/bin/Bookshelf'
```

Linux dient hier als Entwicklungs- und Prüfplattform. Ein App-Image ist ein
startbares Verzeichnis mit Laufzeit. Ein Linux-Store-Release ist nicht geplant.

## Windows x64

Auf Windows mit einem x64-Temurin-JDK 21.0.13+9 und PowerShell ausführen. Für die EXE-Erzeugung
wird WiX Toolset 3.14.1 benötigt, dessen `bin`-Verzeichnis im `PATH` liegt.

```powershell
.\mvnw.cmd verify
.\scripts\package.ps1 -PackageType app-image
& '.\target\app-image\Bookshelf\Bookshelf.exe'
.\scripts\package.ps1 -PackageType exe
```

Die EXE liegt unter `target/dist/buecherregal-1.0-windows-x64.exe`.
Die Installation ist pro Benutzer vorgesehen. Die feste Upgrade-ID ermöglicht
spätere Installer-Updates mit derselben Produktidentität. Die Sammlung liegt
außerhalb des Installationsverzeichnisses.
`assets/branding/icon.ico` wird ausdrücklich beim Aufbau des App-Images verwendet.
Die Darstellung im Windows-Launcher und in den Verknüpfungen ist erst auf
Windows abzunehmen. Das Skript signiert nicht und ist kein Store-Releasehelfer.

## macOS Intel und Apple Silicon

Auf der jeweiligen Mac-Architektur mit passendem Temurin-JDK 21.0.13+9 ausführen:

```bash
./mvnw verify
./scripts/package.sh app-image
open 'target/app-image/Bookshelf.app'
./scripts/package.sh dmg
```

Das Ergebnis heißt `target/dist/buecherregal-1.0-macos-x64.dmg` oder
`target/dist/buecherregal-1.0-macos-arm64.dmg`.
Die Bundle-Kennung ist `de.appfabrik.buecherregal`.
Die aktuellen Pakete haben keine Distributionssignatur und sind als lokale
Testpakete vorbereitet. Plattform-Sicherheitsdialoge sind bei der späteren
nativen Abnahme zu berücksichtigen.

## Nachweis und CI

Die Skripte starten den erzeugten Launcher mit `--version`. Unter Windows
wird dessen Exitcode geprüft, weil ein GUI-Launcher keine verlässliche
Konsolenausgabe liefert. Danach muss auf jeder Plattform der vollständige
sichtbare Ablauf aus `verification.md` folgen, einschließlich Datenbankzugriff
und erneutem Start. Eine Launcher-Prüfung allein belegt keine funktionierende GUI.

Vorhandene versionierte EXE- und DMG-Dateien werden nicht still überschrieben.
Die generierten Zwischenverzeichnisse unter `target` werden neu aufgebaut.

Der GitHub-Workflow `Verify` prüft Java und SQLite auf Linux, Windows und macOS.
Der [Lauf vom 28. Oktober 2026](https://github.com/wasiliy-strecker/bookshelf/actions/runs/40650866023)
bestand auf allen drei Systemen mit 33 Tests für Commit `36bd305`. Dieser
Quellstand liegt vor Internationalisierung und JSON-Transfer. Er bestätigt
weder die aktuelle Anwendung noch einen Installer. Das Remote ist
`https://github.com/wasiliy-strecker/bookshelf`.
`Build native test packages` wird manuell gestartet und erzeugt Windows-x64-,
macOS-x64- und macOS-arm64-Testartefakte. Er erstellt keine GitHub-Releases
und lädt nichts in einen Store. Für diesen Paketierungsworkflow liegt bisher
kein Ausführungsnachweis vor. Auch die neuen JDK-Pins sind noch nicht in CI gelaufen.

## Offene Store-Schritte

Windows und macOS sind lokal noch nicht nativ geprüft. Vor einer Veröffentlichung
sind Installation, Start, Schrift, Tastenkürzel, Speicherung und Updates auf
Zielsystemen abzunehmen. Eine EXE oder DMG allein belegt keine Store-Eignung.

Am 30. Oktober 2026 wurden zwei Vertriebswege aus demselben App-Code festgelegt:

| Ziel | Paket | Signierung und Updates |
| --- | --- | --- |
| Microsoft Store | MSIX | Microsoft signiert die Store-Lieferung und stellt Store-Updates bereit |
| Kostenloser GitHub-Download | EXE | Eigene Signierung und Updateversorgung sind vor öffentlicher Freigabe zu klären und zu prüfen |

Die MSIX-Paketierung ist seit 31. Oktober 2026 als Windows-Helfer implementiert,
aber noch nicht ausgeführt. Die [MSIX-Anleitung](microsoft-store-release.md)
enthält die Konfiguration, Paketierung und manuelle Abnahme. Produktregistrierung,
reservierter Name, echte Package Identity und Publisher aus Partner Center
fehlen noch. Keine Beispielkennung als Produktionsidentität verwenden.
Die App-Version für die erste Veröffentlichung ist `1.0`.
Die vierteilige MSIX-Paketversion muss die Store-Regeln erfüllen. Die Java-
Paketkennung und Installer-Upgrade-ID ersetzen keine Partner-Center-Identität.

`scripts/package_msix.ps1` baut ohne Tests und ohne Launcher-Start ein neues
Windows-App-Image und verpackt es mit MakeAppx. Die Beispielkonfiguration enthält
bewusst leere Store-Identitäten. Die vorgeschlagene MSIX-Version ist `1.0.0.0`.
Store-Upload, lokale Testsignierung und Installation werden nicht ausgeführt.
Der bestehende Windows-Pakethelfer unterstützt dafür `-SkipLauncherCheck`.
Im normalen EXE-/App-Image-Aufruf bleibt die bisherige Launcher-Prüfung aktiv.

Die vorhandene EXE-Vorbereitung bleibt für GitHub erhalten. Ein kostenloser
Download legt keine neue Open-Source-Lizenz für den eigenen Code fest.
Es gibt keinen automatischen Wechsel zwischen Store- und GitHub-Installation.
Die MSIX-Virtualisierung kann Speicherorte und Deinstallationsverhalten ändern.
Beide Wege müssen daher separat mit synthetischen Sammlungen geprüft werden.

Vor einer Windows-Freigabe bleiben folgende Nachweise erforderlich:

- Aktuelles `verify`, Windows-Paket mit eingebettetem Java, Versions- und
  Architekturprüfung sowie vollständige Fremdkomponenten- und Lizenzinventur.
- Installation als Standardbenutzer, Offline-Start ohne installiertes Java,
  sechs Sprachen, Schrift, Tastatur, Skalierung und native JSON-Dateidialoge.
- Update zwischen zwei Paketversionen mit identischen Büchern, IDs, Notizen
  und Spracheinstellung. Tatsächliche Speicherorte und Verhalten bei
  Deinstallation, Neuinstallation und MSIX-Reset dokumentieren. Nutzerdaten
  nicht zur Durchführung dieser Prüfungen löschen.
- MSIX-Zertifizierungsprüfung einschließlich WACK, endgültige Windows-Bilder,
  Kategorie, Altersfreigabe, Märkte, Datenschutzangaben und Zertifizierungshinweise.
- Passender vollständiger Laufzeitquellcode und Begleitinformationen gemäß
  `THIRD_PARTY_NOTICES.md`. Öffentliche Bereitstellung gehört zum Releaseauftrag.

Die GitHub-EXE wird nicht vom Store signiert oder aktualisiert. Würde künftig
auch eine EXE im Store angeboten, wären zusätzlich signierte PE-Dateien,
stille Offline-Installation und unveränderliche versionierte HTTPS-URLs nötig.

Die nächsten Schritte erfolgen in dieser Reihenfolge:

1. Auf Windows den aktuellen Quellstand mit der festgelegten Laufzeit prüfen.
   Im Partner Center die tatsächliche Produktidentität und den Publisher erfassen.
2. MSIX aus dem Windows-App-Image erstellen und die Manifestdaten, Paketversion,
   Icons und Signierung prüfen. Die GitHub-EXE separat bauen und prüfen.
3. Auf einem sauberen Windows-Testsystem Installation, Offline-Betrieb, JSON,
   alle sechs Sprachen und ein Update zwischen zwei Versionen abnehmen.
   Datenpfade, Deinstallation und Neuinstallation mit synthetischen Daten belegen.
4. WACK ausführen, endgültige Windows-Screenshots aufnehmen und Store-Angaben
   einschließlich Datenschutz, Altersfreigabe und Zertifizierungshinweisen abschließen.
5. Vollständige Laufzeitquellen und Lizenzbegleitmaterial den fertigen Artefakten
   zuordnen. Erst nach diesen Nachweisen ist eine Veröffentlichung zu entscheiden.

## Datenschutz für App und Store

Die vollständige Erklärung ist die englische `PRIVACY.md`. App und alle
Store-Sprachen verwenden https://github.com/wasiliy-strecker/bookshelf/blob/main/PRIVACY.md.
`python3 scripts/export_privacy.py neuer-ordner` kopiert die Originaldatei und
ihre URL in eine neue Übergabe. Dafür ist kein App-Build erforderlich.

Aktuelle Microsoft-Quellen:

- [MSIX und EXE im Vergleich](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/choose-distribution-path)
- [MSIX-Paketanforderungen und Signierung](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msix/app-package-requirements)
- [Datenschutztext und Supportangaben](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msix/support-info)
- [AppData-Virtualisierung](https://learn.microsoft.com/en-us/windows/msix/desktop/flexible-virtualization)
- [EXE-Updates erreichen bestehende Installationen nicht durch den Store](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msi/publish-update-to-your-app-on-store)

## Mac App Store

Für den Mac App Store bleiben Apple-Entwickleridentität, App-Sandbox,
Entitlements für Java und native Bibliotheken, Signierung des vollständigen
Bundles und ein geeignetes Einreichungspaket offen. Insbesondere sind der
containerbezogene Datenpfad und das Laden der nativen SQLite-Bibliothek in der
Sandbox auf einem Mac zu prüfen. Eine notarialisierte Direktdistribution ist
ein eigener Weg und ersetzt keine Mac-App-Store-Abnahme.

Quellen für die später erneut zu prüfenden Vorgaben:

- [JDK 21 Packaging Overview](https://docs.oracle.com/en/java/javase/21/jpackage/packaging-overview.html)
- [Apple: Distribution und App Sandbox](https://help.apple.com/xcode/mac/current/en.lproj/dev91fe7130a.html)
- [Microsoft: Store-Überblick](https://learn.microsoft.com/en-us/windows/apps/publish/)
- [GitHub: Verfügbare Runner](https://docs.github.com/en/actions/how-tos/write-workflows/choose-where-workflows-run/choose-the-runner-for-a-job)

## Sprachdaten der eingebetteten Laufzeit

Die Paketierung nimmt `jdk.localedata` ausdrücklich in das Runtime-Image auf.
Dadurch stehen regionale Zahlenformate und Sortierregeln auch ohne separat
installiertes JDK zur Verfügung. Diese Einstellung gilt für beide vorbereiteten
Paketierungsskripte. Die Änderung wurde hier mit dem Linux-App-Image geprüft.
Ein Windows- oder macOS-Paket wurde in diesem Internationalisierungsschritt
nicht gebaut oder nativ abgenommen.

## Mitgelieferte UI-Schrift, 31. Oktober 2026

Das App-JAR enthält Noto Sans CJK JP 2.004 als Regular und Bold einschließlich
OFL-Lizenz. Die Oberfläche lädt diese Dateien vor der ersten Anwendung des
Stylesheets. Die beiden Schriften belegen zusammen rund 32 MiB unkomprimiert.
Es gibt keinen Schrift-Download zur Laufzeit und keine Systeminstallation.
Herkunft und Prüfsummen stehen in `third-party-licenses/noto-sans-cjk/NOTICE.md`.
Die Pakethelfer kopieren diesen Ordner in das Legal-Verzeichnis.
Native Darstellung unter Windows und macOS ist weiterhin separat abzunehmen.

## Plattform-Icons und Desktop-Zuordnung, 31. Oktober 2026

Das vorhandene Motiv unter `assets/branding/icon-1024.png` bleibt die Grundlage.
`scripts/prepare_icons.py` erzeugt mit Python 3 und Pillow zusätzliche PNG-Größen,
ein ICO mit 16, 32, 48, 64, 128 und 256 Pixeln sowie ICNS bis 1024 Pixel.
Die bisherigen Branding-Generatoren rufen diesen Schritt am Ende ihrer
Icon-Erzeugung auf. Für normale Builds werden die eingecheckten Dateien genutzt.
Pillow ist keine Laufzeitabhängigkeit der App.

JavaFX erhält Fenstericons in sechs Größen. Auch Startfehler verwenden das
App-Icon. Der Windows-Helfer übergibt das ICO ausdrücklich an App-Image und
EXE-Installer. Der macOS-Helfer übergibt das ICNS an App-Bundle und DMG-Erzeugung.
Bestehende Paket-IDs und Windows-Upgrade-UUIDs bleiben erhalten.

Linux-App-Images enthalten `install_desktop.py` und `lib/app/desktop-icons/`.
Der Installer schreibt ausschließlich in die XDG-Datenverzeichnisse des aktuellen
Benutzers. Er registriert `de.appfabrik.buecherregal.desktop`, PNG-/SVG-Icons im
Hicolor-Verzeichnis und `StartupWMClass=de.appfabrik.buecherregal.ui.BooksApplication`.
Diese Klasse entspricht der vorhandenen JavaFX-Anwendung. Der native Launcher
wird direkt aufgerufen, ohne Maven, Shell-Wrapper oder systemweites Java.
Bereits vorhandene, nicht von diesem Helfer verwaltete Einträge werden erhalten.
Die optionale Angabe `--data-dir` wird als eigenes Argument korrekt maskiert.
Nach Verschieben des App-Images muss der Eintrag erneut registriert werden.

Windows- und macOS-Iconanzeige sind erst mit den nativen Paketen abzunehmen.
Die Dateien und Skriptänderungen allein ersetzen diese Abnahme nicht. Es wurden
keine Store-Pakete veröffentlicht und keine CI-Workflows gestartet.

Referenzen:

- [JDK 21 jpackage: Plattformpakete und Icon-Option](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jpackage.html)
- [Desktop Entry: Icon und StartupWMClass](https://specifications.freedesktop.org/desktop-entry/latest/recognized-keys.html)
- [Desktop Entry: Maskierung von Exec-Argumenten](https://specifications.freedesktop.org/desktop-entry/latest/exec-variables.html)
- [Apple: Hochauflösende App-Icons](https://developer.apple.com/library/archive/documentation/GraphicsAnimation/Conceptual/HighResolutionOSX/Optimizing/Optimizing.html)

## Mac App Store ab Version 1.0

Der DMG-Weg oben ist kein Mac-App-Store-Paket. Für die gemeinsame Ausgabe
für Apple Silicon und Intel gilt die [Mac-Store-Anleitung](mac-app-store-release.md).
Öffentlicher Name ist Bookshelf. Der ältere DMG-Helfer verwendet
wegen der Längenbegrenzung von `--mac-package-name` den kurzen Menünamen Bookshelf.
Datenverzeichnisse und Bundle-ID bleiben unverändert.
