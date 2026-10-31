package de.appfabrik.buecherregal.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.buecherregal.domain.BookDraft;
import de.appfabrik.buecherregal.i18n.AppLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class LanguagePreferencesTest {
  @TempDir Path directory;

  @Test
  void absentPreferencesStartInEnglishEvenOnGermanSystemsWithoutCreatingFiles() throws Exception {
    String choice = new LanguagePreferences(directory).load();
    assertEquals("en-US", choice);
    assertEquals(AppLanguage.ENGLISH, AppLanguage.resolve(choice, java.util.Locale.GERMANY));
    assertFalse(Files.exists(directory.resolve("language.txt")));
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void savedChoiceSurvivesNewInstancesWithoutTouchingExistingCollection(AppLanguage language)
      throws Exception {
    Path database = directory.resolve("buecherregal.db");
    var repository = new SqliteBookRepository(database);
    repository.initialize();
    var book =
        repository.create(new BookDraft("L'été – Bücher – ação", "Élise", "São Paulo", "one\ntwo"));
    byte[] before = Files.readAllBytes(database);
    new LanguagePreferences(directory).save(language.tag());
    assertEquals(language.tag(), new LanguagePreferences(directory).load());
    assertArrayEquals(before, Files.readAllBytes(database));
    assertEquals(List.of(book), new SqliteBookRepository(database).findAll());
    new LanguagePreferences(directory).save("auto");
    assertEquals("auto", new LanguagePreferences(directory).load());
    try (var files = Files.list(directory)) {
      assertTrue(files.noneMatch(path -> path.toString().endsWith(".tmp")));
    }
  }

  @Test
  void unsupportedOrOversizedSettingFallsBackWithoutOverwritingIt() throws Exception {
    Path file = directory.resolve("language.txt");
    for (String content : List.of("xx-YY", "", "x".repeat(200))) {
      Files.writeString(file, content);
      assertEquals("en-US", new LanguagePreferences(directory).load());
      assertEquals(content, Files.readString(file));
    }
  }

  @Test
  void rejectsInvalidChoiceAndPreservesLastPreference() throws Exception {
    var preferences = new LanguagePreferences(directory);
    preferences.save("fr-FR");
    assertThrows(IllegalArgumentException.class, () -> preferences.save("unsupported"));
    assertEquals("fr-FR", preferences.load());
  }

  @Test
  void blockedDestinationReportsFailureAndCleansTemporaryFiles() throws Exception {
    Path blocked = directory.resolve("language.txt");
    Files.createDirectory(blocked);
    Files.writeString(blocked.resolve("keep.txt"), "preserve");
    assertThrows(IOException.class, () -> new LanguagePreferences(directory).save("fr-FR"));
    assertEquals("preserve", Files.readString(blocked.resolve("keep.txt")));
    try (var files = Files.list(directory)) {
      assertEquals(List.of(blocked), files.toList());
    }
  }
}
