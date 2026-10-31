package de.appfabrik.buecherregal.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppDataDirectoryTest {
  @TempDir Path home;

  @Test
  void resolvesWindowsDataDirectoryAndFallback() {
    Path configured = home.resolve("Local");
    assertEquals(
        configured.resolve("BuecherRegal"),
        AppDataDirectory.resolve(
            "Windows 11", home, Map.of("LOCALAPPDATA", configured.toString())));
    assertEquals(
        home.resolve("AppData/Local/BuecherRegal"),
        AppDataDirectory.resolve("Windows 11", home, Map.of()));
  }

  @Test
  void storeRequiresAnExplicitNativeContainerInsteadOfFallingBackToExistingData() {
    Path container =
        home.resolve(
            "Library/Containers/de.appfabrik.buecherregal/Data/Library/Application Support/BuecherRegal");
    assertEquals(container, AppDataDirectory.sandboxDirectory(container.toString()));
    assertThrows(IllegalArgumentException.class, () -> AppDataDirectory.sandboxDirectory(null));
    assertThrows(IllegalArgumentException.class, () -> AppDataDirectory.sandboxDirectory(""));
    assertThrows(
        IllegalArgumentException.class, () -> AppDataDirectory.sandboxDirectory("relative"));
  }

  @Test
  void resolvesMacDataDirectory() {
    assertEquals(
        home.resolve("Library/Application Support/BuecherRegal"),
        AppDataDirectory.resolve("Mac OS X", home, Map.of()));
  }

  @Test
  void usesAbsoluteXdgDataHomeOrUserFallback() {
    Path configured = home.resolve("xdg");
    assertEquals(
        configured.resolve("buecherregal"),
        AppDataDirectory.resolve("Linux", home, Map.of("XDG_DATA_HOME", configured.toString())));
    List<Map<String, String>> environments =
        List.of(Map.of(), Map.of("XDG_DATA_HOME", ""), Map.of("XDG_DATA_HOME", "relative"));
    for (Map<String, String> environment : environments) {
      assertEquals(
          home.resolve(".local/share/buecherregal"),
          AppDataDirectory.resolve("Linux", home, environment));
    }
  }
}
