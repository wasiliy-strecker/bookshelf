# Verwendete Komponenten

JSON-Import und -Export verwenden Gson 2.13.2 und dessen transitive Abhängigkeit
Error Prone Annotations 2.41.0, beide unter Apache License 2.0.
Die unveränderten Lizenztexte liegen unter `third-party-licenses` und werden
mit den übrigen Hinweisen in jedes App-Image übernommen.
Quellen: [Gson](https://github.com/google/gson/tree/gson-parent-2.13.2)
und [Error Prone](https://github.com/google/error-prone/tree/v2.41.0).

Bookshelf verwendet OpenJFX unter GPLv2 mit Classpath Exception und SQLite
JDBC von Xerial unter Apache License 2.0 und BSD-2-Clause. SQLite selbst ist
Public Domain. Die eingebettete Java-Laufzeit unterliegt den Lizenzbedingungen
der beim Bauen gewählten JDK-Distribution. Die aktuelle Build-Basis ist
Eclipse Temurin 21.0.13+9 unter GPLv2 mit Classpath Exception. Maven prüft
Hersteller, Version und Buildnummer. Das App-Image enthält zusätzlich die
unveränderte JDK-Datei `release` als `legal/BUILD_JAVA_RELEASE.txt`, sodass
Herkunft, Architektur und vollständige Laufzeitversion nachvollziehbar bleiben.

Die vollständigen Hinweise der Laufzeit bleiben im Verzeichnis `legal` des
Runtime-Images erhalten. Bei der bisherigen Desktop-Paketierung bleibt SQLite JDBC als unverändertes
Archiv mit den mitgelieferten Lizenzdateien enthalten. Die OpenJFX-Lizenztexte werden aus
`third-party-licenses` in das Paket übernommen. Diese Datei ergänzt diese Hinweise.

## Versionsbezogene Herkunft und Quellcode

| Komponente | Version | Herkunft und zugehöriger Quellcode |
| --- | --- | --- |
| Eclipse Temurin | 21.0.13+9 | [Binärrelease und vollständiges Quellarchiv](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.13%2B9), Datei `OpenJDK21U-jdk-sources_21.0.13_9.tar.gz` |
| OpenJFX | 21.0.11+4 | [OpenJFX-Quellen](https://github.com/openjdk/jfx21u/tree/21.0.11%2B4), Module `javafx.base`, `javafx.graphics`, `javafx.controls` |
| Xerial SQLite JDBC | 3.53.4.0 | [Unverändertes JDBC-Archiv und Quellen](https://github.com/xerial/sqlite-jdbc/tree/3.53.4.0), Lizenzen unter `META-INF/maven/org.xerial/sqlite-jdbc` im JAR |
| Gson | 2.13.2 | [Quellen](https://github.com/google/gson/tree/gson-parent-2.13.2), Apache License 2.0 |
| Error Prone Annotations | 2.41.0 | [Quellen](https://github.com/google/error-prone/tree/v2.41.0), Apache License 2.0 |

Das JavaFX-Artefakt meldet die Laufzeitversion `21.0.11+4`. Die mitgelieferten
Linux-Nativhinweise decken JPEG und Mesa ab. Die JDK-Lizenzhinweise bleiben
im Runtime-Image erhalten. SQLite selbst ist [Public Domain](https://sqlite.org/copyright.html).
Die Java- und nativen Binärdateien werden nicht funktional verändert.
Der Mac-Store-Helfer ordnet die Archive wie nachfolgend beschrieben neu an.

Die [GPLv2 mit Classpath Exception](https://openjdk.org/legal/gplv2+ce.html)
enthält weiterhin Pflichten für die weitergegebenen Laufzeitkomponenten.
Vor einer öffentlichen Binärlieferung müssen die genau zu den ausgelieferten
Temurin- und OpenJFX-Binärdateien gehörenden vollständigen Quellarchive samt
Buildinformationen und Lizenzhinweisen als Release-Begleitmaterial verfügbar
gemacht werden. Ein bloßer allgemeiner Projektlink ersetzt diese Aufgabe nicht.
Für die spätere Store-Lieferung ist die Bereitstellung aus dem Store-Eintrag
zugänglich zu machen. Für GitHub gehören die Begleitarchive zum zugehörigen
Release. Diese lokale Vorbereitung veröffentlicht weder Binärdateien noch
Quellarchive.

Die zusätzlichen nativen Komponenten des endgültigen Windows-Pakets sind
erst am tatsächlich gebauten Windows-Artefakt abschließend zu inventarisieren.
Die Linux-Prüfung ist keine Lizenz- oder Signaturabnahme eines Windows-Pakets.

Die lokale Store-Vorbereitung vom 31. Oktober 2026 bezieht die vollständigen
Temurin- und OpenJFX-Quellarchive sowie die referenzierten Temurin-Buildskripte
und Windows-Buildmetadaten. Feste URLs und SHA-256 stehen in
`packaging/runtime-sources.json`. `scripts/prepare_store_release.py` übernimmt
sie in `RuntimeSources` der internen Übergabe. Sie sind noch nicht öffentlich
bereitgestellt. Der Abgleich mit den endgültigen Windows-Binärdateien bleibt
weiterhin erforderlich. Details: `packaging/runtime-sources-readme.txt`.

Maven, Spotless, SpotBugs und JUnit sind Entwicklungswerkzeuge und werden nicht
in das App-Image aufgenommen. Dieses Dokument vergibt keine Lizenz für den
eigenen Bookshelf-Quellcode.

## Schrift der Benutzeroberfläche

Die JavaFX-Oberfläche verwendet die unveränderten Regular- und Bold-Dateien
von Noto Sans CJK JP 2.004 unter SIL Open Font License 1.1.
Copyright © 2014–2021 Adobe. Die App lädt die mitgelieferten OTF-Dateien direkt.
Dadurch benötigen die abgedeckten lateinischen und CJK-Zeichen keine auf dem
Zielrechner installierte Schrift. Weitere Zeichen verwenden die systemabhängige
JavaFX-Ersatzschrift. Eine vollständige Unicode- oder Farbemoji-Abdeckung wird
nicht zugesagt.

Lizenz, fester Quellstand und SHA-256 stehen unter
`third-party-licenses/noto-sans-cjk/`. Die Lizenz liegt außerdem bei den
Schriftressourcen im App-JAR. Pakethelfer übernehmen die Hinweise nach
`legal/noto-sans-cjk/`. Die Schrift wird nicht systemweit installiert.

## Mac App Store: beide Architekturen

Die Mac-Vorbereitung verwendet dieselben Temurin- und OpenJFX-Quellstände.
Die ARM64- und Intel-Binärarchive, Herkunftsmetadaten und Prüfsummen sind unter
`packaging/macos` festgehalten. Beide Temurin-Metadaten nennen dieselbe
Buildskriptrevision `1cf43d7a3e77886bfea7d550a902e151a87d724d` wie die vorhandene
Quelllieferung. Originalarchive bleiben als geprüfte Eingaben erhalten.

Für die Signierung werden native JavaFX- und SQLite-Bibliotheken aus den JARs
in architekturspezifische Bundle-Verzeichnisse übertragen. Die ausgelieferten
JARs enthalten weiterhin die unveränderten Klassen und Lizenzdateien, jedoch
keine eingebetteten nativen Bibliotheken. Die Bibliotheken werden separat mit
der Identität des Entwicklers signiert. Diese geänderte Verpackung und alle
Prüfsummen werden im Mac-Baubericht dokumentiert. Es wurden keine JavaFX- oder
SQLite-Quellfunktionen verändert. `jlink` erzeugt zwei eingebettete Laufzeiten.
Der kleine native Launcher ist eigener app-lokaler Code.

Der Übergabeordner enthält die vollständigen vorhandenen Laufzeitquellarchive
und zusätzlich die Mac-Buildmetadaten. Deren öffentliche Bereitstellung,
die Lizenzinventur des tatsächlich gebauten Mac-Bundles und die App-Store-
Einreichung bleiben vor der Veröffentlichung abzuschließen.
