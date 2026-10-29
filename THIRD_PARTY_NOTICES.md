# Verwendete Komponenten

JSON-Import und -Export verwenden Gson 2.13.2 und dessen transitive Abhängigkeit
Error Prone Annotations 2.41.0, beide unter Apache License 2.0.
Die unveränderten Lizenztexte liegen unter `third-party-licenses` und werden
mit den übrigen Hinweisen in jedes App-Image übernommen.
Quellen: [Gson](https://github.com/google/gson/tree/gson-parent-2.13.2)
und [Error Prone](https://github.com/google/error-prone/tree/v2.41.0).

BücherRegal verwendet OpenJFX unter GPLv2 mit Classpath Exception und SQLite
JDBC von Xerial unter Apache License 2.0 und BSD-2-Clause. SQLite selbst ist
Public Domain. Die eingebettete Java-Laufzeit unterliegt den Lizenzbedingungen
der beim Bauen gewählten JDK-Distribution. Für die dokumentierten Builds wird
OpenJDK 21 von Eclipse Temurin unter GPLv2 mit Classpath Exception verwendet.

Die vollständigen Hinweise der Laufzeit bleiben im Verzeichnis `legal` des
Runtime-Images erhalten. SQLite JDBC bleibt als unverändertes Archiv mit den
mitgelieferten Lizenzdateien enthalten. Die OpenJFX-Lizenztexte werden aus
`third-party-licenses` in das Paket übernommen. Diese Datei ergänzt diese Hinweise.

Quellen:

- https://openjdk.org/legal/gplv2+ce.html
- https://github.com/openjdk/jfx/blob/jfx21/LICENSE
- https://github.com/xerial/sqlite-jdbc
- https://sqlite.org/copyright.html
- https://adoptium.net/about/

Maven, Spotless, SpotBugs und JUnit sind Entwicklungswerkzeuge und werden nicht
in das App-Image aufgenommen. Dieses Dokument vergibt keine Lizenz für den
eigenen BücherRegal-Quellcode.
