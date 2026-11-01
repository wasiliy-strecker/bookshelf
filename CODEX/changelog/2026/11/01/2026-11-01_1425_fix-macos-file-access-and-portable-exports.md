# Support collection exports without hard links

App-lokalen Ausweichweg für neue JSON-Dateien ergänzt. Exklusive Neuanlage
verhindert unbestätigtes Überschreiben. Schreibfehler bereinigen nur die eigene,
noch identifizierbare Teildatei. Bestätigter Dateiersatz bleibt atomar.
Spracheinstellungen, Mac-Ordnerwahl und persönliche Exporte unverändert.

124 Tests einschließlich UI und verweigerter nativer Export-Hardlinks bestanden.
Spotless, Compiler-Lint, Enforcer und SpotBugs bestanden. Native Mac-/exFAT-Abnahme
und Grenzen des Kopierfallbacks stehen in docs/verification.md.
Checkpoint vor Umsetzung. Abschlusscommit nach Prüfung. Kein Push oder Store-Bau.
