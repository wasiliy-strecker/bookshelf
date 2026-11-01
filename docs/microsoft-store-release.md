# Bookshelf für den Microsoft Store vorbereiten

Stand: 31. Oktober 2026. **Noch nicht bereit.** Diese Anleitung betrifft nur
Bookshelf. Der lokale MSIX-Helfer ist implementiert, aber noch nicht auf
Windows ausgeführt. Der Nutzer hat noch keine App im Partner Center angelegt.
Es gibt deshalb keine bestätigte Store-Identität und kein fertiges MSIX.
In diesem Arbeitsschritt werden auf ausdrücklichen Nutzerwunsch keine Tests
ausgeführt. Die folgenden Prüfschritte sind Aufgaben für die manuelle Abnahme.

## Was hier vorbereitet ist

- Windows-x64-MSIX-Helfer mit vollständigem jpackage-App-Image und Java-Laufzeit.
- Manifest mit sechs Sprachen, Desktop-Ziel und `runFullTrust` für JavaFX/SQLite.
- PNG-Paketlogos, Windows-ICO und Store-Icon mit 300 × 300 Pixeln.
- Store-Texte und Erstveröffentlichungshinweise in allen sechs Sprachen.
- Datenschutztexte aus denselben Ressourcen wie der Offline-Dialog der App.
- Vollständige Temurin- und OpenJFX-Quellarchive, Herkunft und Buildinformationen
  als lokale Begleitmaterialien. Öffentliche Bereitstellung bleibt offen.
- Übergabeordner mit Quellstand, Texten und dieser Anleitung, ohne ZIP.

Die Vorlagen sind keine bestandene Zertifizierung. Vorhandene Linux-Aufnahmen
bleiben Vorschauen. Finale Windows-Aufnahmen werden separat erstellt.

## 1. App im Partner Center anlegen

Windows-App als MSIX/APPX anlegen und den Namen Bookshelf reservieren.
Aus der Produktidentität exakt übernehmen:

| Partner Center | `store.local.json` |
| --- | --- |
| Package/Identity/Name | IdentityName |
| Package/Identity/Publisher | Publisher |
| PublisherDisplayName | PublisherDisplayName |
| Reservierter Produktname | DisplayName |

`packaging/windows/store.example.json` nach `store.local.json` im selben Ordner
kopieren. Die drei leeren Identitätsfelder mit echten Werten füllen. Diese
Werte sind keine Zugangsdaten. Die lokale Konfiguration wird nicht committet.
Ein syntaktisch gültiger Wert allein beweist keine Zuordnung im Partner Center.

Die vorgeschlagene erste Paketversion ist `1.0.0.0`. Die App-Version im Maven-
Projekt ist `1.0`. Beide stehen später in `build-info.json`. Microsoft
verlangt vier Zahlen von 0 bis 65535, die erste größer als 0 und die letzte 0.
Für spätere Pakete die ersten drei Teile erhöhen, beispielsweise `1.0.1.0`.

Die Vorlage setzt Windows 10 Build 19041 als technische Untergrenze. Die
Vorgabe `MaxVersionTested` ist zunächst ebenfalls 19041. Sie ist kein Nachweis
einer ausgeführten Prüfung. Nach der tatsächlichen Windows-Abnahme den passenden
Wert eintragen und das daraus gebaute endgültige Paket abnehmen.

## 2. Auf Windows bauen

Auf Windows x64 den Übergabeordner `Source` verwenden. Benötigt werden ein
vollständiges Eclipse Temurin JDK **21.0.13+9 für Windows x64**, das Windows SDK
mit `MakeAppx.exe` und PowerShell. `JAVA_HOME` auf das JDK-Verzeichnis setzen.
Maven lädt beim ersten Build Abhängigkeiten. Die fertige App arbeitet offline.
WiX ist für diesen MSIX-Weg nicht erforderlich.

```powershell
.\scripts\package_msix.ps1
```

Optional den SDK-Pfad mit `-MakeAppx 'C:\...\x64\makeappx.exe'` angeben.
Der Helfer baut immer ein neues Windows-App-Image und startet keine App.
Er führt weder Tests noch Installation, Signierung oder Upload aus.
MakeAppx führt seine reguläre Paketvalidierung durch. `/nv` wird nicht benutzt.

Die neue Lieferung liegt unter `build/releases/windows/1.0.0.0/`:

- `buecherregal-1.0.0.0-windows-x64.msix`, zunächst unsigniert für den Store.
- Tatsächliches `AppxManifest.xml`, SHA-256 und JDK-Herkunftsdatei.
- `payload-inventory.csv` mit Dateinamen, Größen und SHA-256 des Paketinhalts.
- `build-info.json` mit explizit offener Abnahme.

Vorhandene Lieferordner werden nicht ersetzt. Auch einen fehlgeschlagenen oder
unvollständigen Ordner vor einem erneuten Lauf bewusst sichern oder umbenennen.
Der Helfer benötigt keine realen Bücherdaten und übernimmt keine Sammlung.

## 3. Lokale Installation mit Testsignatur

Der Microsoft Store signiert MSIX selbst. Ein gekauftes Codesigning-Zertifikat
ist für diesen Vertriebsweg nicht nötig. Für die lokale Installation vor der
Store-Freigabe ist eine vertrauenswürdige Testsignatur erforderlich. Diese
Schritte ausschließlich auf einem eigenen Windows-Testsystem ausführen.

Ein vorhandenes geeignetes Zertifikat kann verwendet werden. Sein Subject muss
exakt dem Manifest-Publisher entsprechen. Andernfalls lokal ein Testzertifikat
erzeugen. Der Schlüssel bleibt im Windows-Zertifikatsspeicher:

```powershell
$settings = Get-Content .\packaging\windows\store.local.json -Raw -Encoding UTF8 | ConvertFrom-Json
$cert = New-SelfSignedCertificate -Type CodeSigningCert -Subject $settings.Publisher `
  -CertStoreLocation Cert:\CurrentUser\My -KeyAlgorithm RSA -KeyLength 2048 `
  -HashAlgorithm SHA256 -KeyExportPolicy NonExportable
Export-Certificate -Cert $cert -FilePath .\build\BuecherRegal-local-test.cer
```

Nur das öffentliche CER auf dem Testsystem über den Windows-Zertifikatimport
nach „Lokaler Computer“ → „Vertrauenswürdige Personen“ importieren. Dafür sind
Administratorrechte nötig. Den privaten Schlüssel nicht verteilen. Die spätere
App-Installation und der App-Start sind als Standardbenutzer abzunehmen.

Die unsignierte Store-Datei aufbewahren und eine gesonderte Testkopie signieren.
`signtool.exe` stammt aus dem Windows SDK. Den tatsächlichen SDK-Pfad setzen:

```powershell
$unsigned = '.\build\releases\windows\1.0.0.0\buecherregal-1.0.0.0-windows-x64.msix'
$signed = '.\build\buecherregal-1.0.0.0-local-test.msix'
if (Test-Path $signed) { throw 'Vorhandene Testkopie nicht ersetzen.' }
Copy-Item $unsigned $signed
& $signTool sign /fd SHA256 /sha1 $cert.Thumbprint $signed
if ($LASTEXITCODE -ne 0) { throw 'Signierung fehlgeschlagen.' }
& $signTool verify /pa /v $signed
if ($LASTEXITCODE -ne 0) { throw 'Signatur ungueltig.' }
Add-AppxPackage -Path $signed
```

Für den Folgebuild dieselbe Zertifikatsidentität verwenden. Nicht jedes Mal ein
neues Zertifikat erzeugen. Dieses Verfahren wurde hier nicht ausgeführt.

## 4. Deine manuelle Prüfung

Auf Ubuntu kannst du jetzt die Anwendungsfunktionen prüfen. Ausschließlich eine
eigene Beispielsammlung verwenden. Reale Bücher vor Importversuchen exportieren.
Eine Ubuntu-Abnahme ersetzt die folgenden Windows-Nachweise nicht.

| Prüfschritt | Erwartung | Ergebnis |
| --- | --- | --- |
| Buch anlegen, bearbeiten, suchen, löschen und jeweils abbrechen | Richtige Einträge, Notizen und unveränderte Daten beim Abbruch | offen |
| Titel „Bücher 日本語“, Autor „Ömer“, Akzente und mehrzeilige Notiz | Lesbar in Tabelle und Bearbeitung. Emoji separat beurteilen | offen |
| Export zweimal mit einigen Sekunden Abstand | Name enthält Datum und Uhrzeit mit Sekunden | offen |
| Export in getrennte leere Beispielsammlung importieren, danach nochmals | Gleiche Daten, wiederholter Import erzeugt keine zusätzlichen identischen Exemplare | offen |
| Ungültige JSON-Datei importieren und Dateidialog abbrechen | Verständliche Meldung beziehungsweise Abbruch, Sammlung unverändert | offen |
| Alle sechs Sprachen nacheinander, jeweils App neu öffnen | Vollständige Texte, Sprachwahl gespeichert, Bücher unverändert | offen |
| Info → Datenschutz, scrollen, kopieren, Escape und Schließen | Vollständiger Offline-Text, passende Sprache, Dialog ohne sichtbaren Sprung | offen |
| Fokus mit Tab auf Bearbeiten und Löschen, App-Icon | Bearbeiten grün, Löschen rot. Eigenes App-Icon | offen |

Zusätzlich **auf echtem Windows** mit dem gebauten MSIX und synthetischen Daten:

| Prüfschritt | Erforderlicher Nachweis | Ergebnis |
| --- | --- | --- |
| Installation als Standardbenutzer | Erfolgreiche MSIX-Installation, Startmenü und Taskleiste mit Icon | offen |
| Saubere Windows-Installation ohne zusätzliches Java, offline | App startet, speichert, sucht und öffnet alle Dialoge mit mitgelieferter Laufzeit | offen |
| Alle obigen Funktionsschritte, native Dateidialoge, 100/150/200 % Skalierung | Lesbare Oberfläche, keine abgeschnittenen Aktionen | offen |
| Aktualisierung von `1.0.0.0` auf `1.0.1.0` | Gleiche Identität und Signatur. Bücher, IDs, Notizen und Sprache nach Update und Neustart erhalten | offen |
| Tatsächlicher Datenpfad und Schreibrechte | MSIX-Virtualisierung dokumentiert, SQLite inklusive nativer Bibliothek funktioniert | offen |
| Deinstallation, Neuinstallation und Reset in wegwerfbarer VM | Verhalten dokumentiert, Wiederherstellung aus JSON möglich. Keine echten Daten löschen | offen |
| Windows App Certification Kit, finales Paket | Bericht gespeichert und Blocker behoben | offen |
| Fremdkomponenten des fertigen Windows-Pakets | DLL-/JAR-Inventar, Runtime-Legal-Verzeichnis und zugehörige Quellen zugeordnet | offen |

Das Upgrade mit `Add-AppxPackage` über die vorhandene Installation ausführen.
Nicht vorher deinstallieren. Zur Updateprüfung JSON vor und nach dem Update
vergleichen. Für interne IDs zusätzlich die synthetische Datenbank sichern
und vergleichen. Kein Downgrade einer produktiven Sammlung versuchen.
Der spätere echte Store-Installations-/Updateweg bleibt zusätzlich abzunehmen.

Bei jedem Ergebnis Datum, Windows-Build, Paket-SHA-256, Paketversion und
Beobachtung notieren. Historische CI-Ergebnisse gelten nicht für dieses Paket.

## 5. Store-Unterlagen abschließen

`Store/<Sprache>/` enthält direkt kopierbare Feldtexte. `Privacy/` enthält die
vollständigen sechs Erklärungen aus den App-Ressourcen. Laut Microsoft kann im
MSIX-Supportbereich auch der Erklärungstext direkt angegeben werden. Die
In-App-Erklärung bleibt zusätzlich erhalten. Die Angaben im Partner Center
müssen dieselbe Datenverarbeitung beschreiben. „Offline“ allein beantwortet
die Frage nach Zugriff auf personenbezogene Inhalte nicht mit „Nein“, denn
selbst eingegebene Bücher und Notizen können personenbezogene Angaben enthalten.

`store-listings/submission/` enthält Supportkontakt und englische Hinweise für
die Zertifizierung. Preis, Märkte, Kategorie und Altersfragebogen sind im
Partner Center noch auszufüllen. Keine automatische Veröffentlichung vorsehen.

Screenshots aus der finalen Windows-App aufnehmen. Desktop-PNG mindestens
1366 × 768 Pixel und höchstens 50 MB. Geplant sind vier Bilder pro Sprache
in 1920 × 1080: Sammlung, Bearbeitung, Suche und Sprachauswahl. Die vorhandenen
Bildunterschriften passen zu dieser Reihenfolge. Nur synthetische Bücher zeigen.
Keine zusätzlichen Marketingtexte in die Bilder einbauen. Die Übergabe enthält
bewusst noch keine als endgültig ausgewiesenen Screenshots.

Die passenden Runtime-Quellen müssen vor öffentlicher Binärverteilung auch
öffentlich erreichbar sein. `RuntimeSources` ist dafür lokales Material, noch
keine öffentliche Bereitstellung. Die abschließende Zuordnung zu den tatsächlich
verwendeten Windows-Binärdateien und deren nativen Drittkomponenten bleibt offen.

## Offizielle Grundlagen

Am 31. Oktober 2026 herangezogen:

- [MSIX-Paketaufbau und Desktop-Manifest](https://learn.microsoft.com/en-us/windows/msix/desktop/desktop-to-uwp-manual-conversion)
- [MakeAppx und seine Paketvalidierung](https://learn.microsoft.com/en-us/windows/msix/package/create-app-package-with-makeappx-tool)
- [Store-Paketversion, Signierung und Zertifizierung](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msix/app-package-requirements)
- [SignTool für MSIX](https://learn.microsoft.com/en-us/windows/msix/package/sign-msix-package-guide)
- [Datenschutz und Support](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msix/support-info)
- [Screenshots und Store-Bilder](https://learn.microsoft.com/en-us/windows/apps/publish/publish-your-app/msix/screenshots-and-images)
- [Verhalten paketierter Desktop-Apps](https://learn.microsoft.com/en-us/windows/msix/desktop/desktop-to-uwp-behind-the-scenes)
