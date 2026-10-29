package de.appfabrik.buecherregal.persistence;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.buecherregal.application.CollectionTransfer;
import de.appfabrik.buecherregal.application.TransferException;
import de.appfabrik.buecherregal.domain.BookDraft;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CollectionTransferTest {
  @TempDir Path directory;
  private Path database;
  private SqliteBookRepository repository;
  private CollectionTransfer transfer;
  private final BookDraft unicode =
      new BookDraft(
          "L'été – Bücher 📚 日本語", "Élise O'Connor", "Küche", "  Erste\r\n\nZweite\tZeile  ");

  @BeforeEach
  void setup() {
    database = directory.resolve("buecherregal.db");
    repository = new SqliteBookRepository(database);
    repository.initialize();
    transfer = new CollectionTransfer(repository, new JsonCollectionFile(directory));
  }

  @Test
  void exportsTheWholeSavedCollectionAndRestoresAllFieldsAndDuplicateCopies() throws Exception {
    repository.create(unicode);
    repository.create(unicode);
    repository.create(new BookDraft("Other", "", "", ""));
    var original = repository.findAll();
    byte[] databaseBytes = Files.readAllBytes(database);
    Path file = directory.resolve("Sammlung für Ömer.json");
    assertEquals(3, transfer.exportBooks(transfer.inspect(file), false));
    assertArrayEquals(databaseBytes, Files.readAllBytes(database));
    var restored = new SqliteBookRepository(directory.resolve("restored.db"));
    restored.initialize();
    var result = restored.importBooks(transfer.readImport(file));
    assertEquals(3, result.added());
    assertEquals(0, result.skipped());
    assertEquals(
        original.stream().map(book -> book.draft()).toList(),
        result.books().stream().map(book -> book.draft()).toList());
    assertEquals(
        result.books(), new SqliteBookRepository(directory.resolve("restored.db")).findAll());
  }

  @Test
  void preservesExistingIdentitiesAndAddsOnlyMissingCopiesOnRepeatedImport() {
    var existing = repository.create(unicode);
    var other = repository.create(new BookDraft("Already here", "", "", ""));
    var changed =
        new BookDraft(unicode.title(), unicode.author(), unicode.location(), "Changed note");
    var result = transfer.importBooks(List.of(unicode, unicode, changed));
    assertEquals(2, result.added());
    assertEquals(1, result.skipped());
    assertTrue(result.books().contains(existing));
    assertTrue(result.books().contains(other));
    var repeated = transfer.importBooks(List.of(unicode, unicode, changed));
    assertEquals(0, repeated.added());
    assertEquals(3, repeated.skipped());
    assertEquals(result.books(), repeated.books());
  }

  @Test
  void rollsBackEarlierInsertsWhenALaterBookFailsAndCanRetry() throws Exception {
    var existing = repository.create(unicode);
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute(
          "CREATE TRIGGER fail_later BEFORE INSERT ON books WHEN NEW.title = 'Broken' BEGIN SELECT RAISE(ABORT, 'test failure'); END");
    }
    List<BookDraft> input =
        List.of(new BookDraft("First", "", "", ""), new BookDraft("Broken", "", "", ""));
    var error = assertThrows(TransferException.class, () -> transfer.importBooks(input));
    assertEquals(TransferException.Code.IMPORT, error.code());
    assertEquals(List.of(existing), repository.findAll());
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("DROP TRIGGER fail_later");
    }
    assertEquals(2, transfer.importBooks(input).added());
    assertEquals(3, repository.findAll().size());
  }

  @Test
  void lockedDatabaseImportsNothingAndSucceedsAfterUnlock() throws Exception {
    var existing = repository.create(unicode);
    List<BookDraft> input = List.of(new BookDraft("New", "", "", ""));
    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      assertEquals(
          TransferException.Code.IMPORT,
          assertThrows(TransferException.class, () -> transfer.importBooks(input)).code());
      statement.execute("ROLLBACK");
    }
    assertEquals(List.of(existing), repository.findAll());
    assertEquals(1, transfer.importBooks(input).added());
  }

  @Test
  void emptyCollectionRoundTripsWithoutDeletingExistingBooks() {
    Path file = directory.resolve("empty.json");
    assertEquals(0, transfer.exportBooks(transfer.inspect(file), false));
    var existing = repository.create(unicode);
    var result = transfer.importBooks(transfer.readImport(file));
    assertEquals(0, result.added());
    assertEquals(List.of(existing), result.books());
  }
}
