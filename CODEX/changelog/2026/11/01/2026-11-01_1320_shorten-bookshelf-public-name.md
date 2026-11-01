# Shorten the public app name to Bookshelf

Auf Nutzerauftrag den Zusatz „by Appfabrik“ aus dem öffentlichen Namen
entfernt. Oberfläche, sechs Übersetzungen einschließlich Datenschutz,
Launcher, lokale Store-Texte und Windows-/Mac-Paketvorlagen heißen Bookshelf.
Root- und App-Regeln halten die neue Namensentscheidung fest.

Die fünf Java-Apps besitzen bereits separate Datenschutzdialoge. Drei Einträge
unter Info für App-Informationen, Datenschutz und Lizenzen wurden empfohlen,
aber noch nicht implementiert. Die C++-App wurde in diesem Auftrag nicht geändert.

Prüfung: 107 Java-Tests, fünf Python-Tests, Compiler-Lint, Spotless und SpotBugs
bestanden. Echte GUI-Abläufe auf Deutsch und Englisch mit Datenschutz und
Sprachwechsel/Neustart bestanden. Neues Linux-App-Image aus isolierten Quellen
gebaut, JAR-Klassen und Ressourcen abgeglichen, Fenstername und natives
Schließen geprüft. Shell-, Python-, JSON-/XML-Syntax und Diff geprüft.

Linux-Anwendungseintrag aktualisiert und bisherigen Datenpfad erhalten.
Technische Kennungen, Dateiformate, Version und Nutzerdateien unverändert.
Historische Lieferungen behalten ihren damaligen Namen. Online-Store-Einträge
und Reservierungen nicht geändert. Kein Push oder Upload. Native Windows-
und Mac-Abnahmen sind nicht Bestandteil dieser Namensänderung.

Checkpoint: `bc87568`. Details: `buecherregal/docs/verification.md`.
