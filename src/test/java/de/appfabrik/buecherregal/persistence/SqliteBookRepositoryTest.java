package de.appfabrik.buecherregal.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.buecherregal.application.BookService;
import de.appfabrik.buecherregal.application.StorageException;
import de.appfabrik.buecherregal.domain.Book;
import de.appfabrik.buecherregal.domain.BookDraft;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteBookRepositoryTest {
  @TempDir Path directory;
  private Path database;
  private SqliteBookRepository repository;
  private BookService service;

  @BeforeEach
  void setUp() {
    database = directory.resolve("Bücher mit Leerzeichen/sammlung.db");
    repository = new SqliteBookRepository(database);
    service = new BookService(repository);
    assertTrue(service.load().isEmpty());
  }

  @Test
  void createsEditsReopensAndDeletesIndependentBooks() {
    Book first = service.save(null, new BookDraft("Der Garten", "Mia", "Regal 2", "Erste\nNotiz"));
    Book second = service.save(null, first.draft());
    assertNotEquals(first.id(), second.id());
    Book edited =
        service.save(first.id(), new BookDraft("Der Garten", "Nora", "Regal 3", "Neue\nNotiz"));

    BookService reopened = new BookService(new SqliteBookRepository(database));
    assertEquals(List.of(edited, second), BookService.search(reopened.load(), ""));
    assertEquals(List.of(edited), BookService.search(reopened.load(), "REGAL 3"));
    assertEquals(List.of(edited, second), BookService.search(reopened.load(), "garten"));
    reopened.delete(edited.id());
    assertEquals(List.of(second), service.load());
    reopened.delete(second.id());
    assertTrue(new BookService(new SqliteBookRepository(database)).load().isEmpty());
  }

  @Test
  void storesQuotesSqlTextUnicodeAndLongMultilineNotesAsData() {
    BookDraft draft =
        new BookDraft(
            "Buch'); DROP TABLE books; --", "O'Connor", "Küche", "ÄÖÜ 日本語 📚\n".repeat(500));
    Book created = repository.create(draft);
    assertEquals(List.of(created), repository.findAll());
    assertEquals(draft, repository.findAll().getFirst().draft());
    Book emptyOptional = repository.create(new BookDraft("Nur Titel", null, null, null));
    assertEquals("", emptyOptional.note());
    assertEquals(2, repository.findAll().size());
  }

  @Test
  void doesNotResurrectMissingEntriesOrTreatMissingDeletesAsSuccess() {
    Book book = repository.create(new BookDraft("Atlas", "", "", ""));
    repository.delete(book.id());
    assertThrows(StorageException.class, () -> repository.update(book.id(), book.draft()));
    assertThrows(StorageException.class, () -> repository.delete(book.id()));
    assertTrue(repository.findAll().isEmpty());
  }

  @Test
  void lockedDatabasePreservesPersistedBookAndRejectsChanges() throws Exception {
    Book original = repository.create(new BookDraft("Original", "", "", "Erhalten"));
    try (var blocker = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = blocker.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      assertThrows(
          StorageException.class, () -> repository.create(new BookDraft("Neu", "", "", "")));
      assertThrows(
          StorageException.class,
          () -> repository.update(original.id(), new BookDraft("Geändert", "", "", "")));
      assertThrows(StorageException.class, () -> repository.delete(original.id()));
      statement.execute("ROLLBACK");
    }
    assertEquals(List.of(original), repository.findAll());
  }

  @Test
  void rejectsNewerSchemaWithoutChangingIt() throws Exception {
    Book original = repository.create(new BookDraft("Vorhanden", "", "", ""));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("PRAGMA user_version = 2");
    }
    assertThrows(StorageException.class, repository::initialize);
    assertEquals(List.of(original), repository.findAll());
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement();
        var result = statement.executeQuery("PRAGMA user_version")) {
      assertTrue(result.next());
      assertEquals(2, result.getInt(1));
    }
  }

  @Test
  void refusesCorruptedDatabaseInsteadOfReplacingIt() throws Exception {
    Path corrupted = directory.resolve("corrupted.db");
    byte[] original = "Keine SQLite-Datenbank".getBytes(java.nio.charset.StandardCharsets.UTF_8);
    Files.write(corrupted, original);
    assertThrows(StorageException.class, () -> new SqliteBookRepository(corrupted).initialize());
    assertArrayEquals(original, Files.readAllBytes(corrupted));
  }

  @Test
  void reportsInvalidDataDirectory() throws Exception {
    Path file = directory.resolve("file");
    Files.writeString(file, "blockiert");
    assertThrows(
        StorageException.class,
        () -> new SqliteBookRepository(file.resolve("books.db")).initialize());
  }
}
