#!/usr/bin/env bash
set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
if [[ $# != 1 ]]; then
  printf 'Aufruf: scripts/export_privacy.sh neuer-ausgabeordner\n' >&2
  exit 1
fi
if [[ -z "${JAVA_HOME:-}" ]]; then
  export JAVA_HOME="$HOME/.local/share/buecherregal/jdk-21.0.13+9"
fi
export PATH="$JAVA_HOME/bin:$PATH"
cd "$ROOT"
./mvnw --batch-mode --no-transfer-progress compile -Dmaven.test.skip=true
mkdir -p target/privacy-export-classes
"$JAVA_HOME/bin/javac" -encoding UTF-8 -cp target/classes -d target/privacy-export-classes \
  src/test/java/de/appfabrik/buecherregal/i18n/PrivacyTextExport.java
"$JAVA_HOME/bin/java" -cp target/classes:target/privacy-export-classes \
  de.appfabrik.buecherregal.i18n.PrivacyTextExport "$1"
