package de.appfabrik.buecherregal.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import de.appfabrik.buecherregal.application.TransferException;
import de.appfabrik.buecherregal.domain.BookDraft;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class JsonCollectionFileTest {
  @TempDir Path directory;
  private JsonCollectionFile files;
  private final BookDraft book = new BookDraft("Atlas", "Author", "Shelf", "Note\nNext line");
  private static final String VALID =
      "{\"format\":\"bookshelf.collection\",\"version\":1,\"books\":[{\"title\":\"Atlas\",\"author\":\"\",\"location\":\"\",\"note\":\"\"}]}";

  @BeforeEach
  void setup() {
    files = new JsonCollectionFile(directory);
  }

  static Stream<String> invalidFiles() {
    return Stream.of(
        "",
        "[]",
        "{}",
        VALID + "{}",
        VALID.substring(0, VALID.length() - 1),
        VALID.replace("\"title\":\"Atlas\"", "\"title\":\" \""),
        VALID.replace("\"title\":\"Atlas\"", "\"title\":null"),
        VALID.replace("\"title\":\"Atlas\"", "\"title\":123"),
        VALID.replace("\"title\":\"Atlas\"", "\"title\":[]"),
        VALID.replace("\"author\":\"\",", ""),
        VALID.replace("\"title\":\"Atlas\"", "\"title\":\"Atlas\",\"title\":\"Other\""),
        VALID.replace("\"version\":1", "\"version\":1,\"version\":1"),
        VALID.replace("\"version\":1", "\"version\":\"1\""),
        VALID.replace("bookshelf.collection", "another.app"),
        VALID.replace("\"note\":\"\"", "\"unknown\":\"\""),
        VALID.replace("\"note\":\"\"", "\"note\":\"\", "),
        VALID.replace("\"title\":\"Atlas\"", "\"title\":\"\\ud800\""),
        VALID.replace("\"title\"", "title"),
        "//comment\n" + VALID);
  }

  @ParameterizedTest
  @MethodSource("invalidFiles")
  void rejectsMalformedWrongAndAmbiguousDocuments(String text) throws Exception {
    Path file = directory.resolve("invalid.json");
    Files.writeString(file, text);
    assertEquals(
        TransferException.Code.FORMAT,
        assertThrows(TransferException.class, () -> files.read(file)).code());
  }

  @Test
  void rejectsUnknownVersionAndBrokenUtf8() throws Exception {
    Path file = directory.resolve("invalid.json");
    Files.writeString(file, VALID.replace("\"version\":1", "\"version\":2"));
    assertEquals(
        TransferException.Code.VERSION,
        assertThrows(TransferException.class, () -> files.read(file)).code());
    Files.write(file, new byte[] {(byte) 0xc3, 0x28});
    assertEquals(
        TransferException.Code.FORMAT,
        assertThrows(TransferException.class, () -> files.read(file)).code());
  }

  @Test
  void requiresExplicitReplacementAndRejectsChangedTargets() throws Exception {
    Path file = directory.resolve("backup.json");
    Files.writeString(file, "previous export");
    var target = files.inspect(file);
    assertEquals(
        TransferException.Code.CONFIRM,
        assertThrows(TransferException.class, () -> files.write(List.of(book), target, false))
            .code());
    assertEquals("previous export", Files.readString(file));
    Files.writeString(file, "externally changed export");
    assertEquals(
        TransferException.Code.CHANGED,
        assertThrows(TransferException.class, () -> files.write(List.of(book), target, true))
            .code());
    assertEquals("externally changed export", Files.readString(file));
    files.write(List.of(book), files.inspect(file), true);
    assertEquals(List.of(book), files.read(file));
  }

  @Test
  void doesNotOverwriteANewFileAppearingAfterSelection() throws Exception {
    Path file = directory.resolve("new.json");
    var target = files.inspect(file);
    Files.writeString(file, "someone else's file");
    assertEquals(
        TransferException.Code.CHANGED,
        assertThrows(TransferException.class, () -> files.write(List.of(book), target, false))
            .code());
    assertEquals("someone else's file", Files.readString(file));
  }

  @Test
  void sizeFailurePreservesOldExportAndRemovesTemporaryFiles() throws Exception {
    Path file = directory.resolve("backup.json");
    Files.writeString(file, "previous export");
    var large = new BookDraft("Large", "", "", "x".repeat(JsonCollectionFile.MAX_BYTES));
    assertEquals(
        TransferException.Code.LIMIT,
        assertThrows(
                TransferException.class,
                () -> files.write(List.of(large), files.inspect(file), true))
            .code());
    assertEquals("previous export", Files.readString(file));
    try (var paths = Files.list(directory)) {
      assertEquals(List.of(file), paths.toList());
    }
    Files.writeString(file, "x".repeat(JsonCollectionFile.MAX_BYTES + 1));
    assertEquals(
        TransferException.Code.LIMIT,
        assertThrows(TransferException.class, () -> files.read(file)).code());
  }

  @Test
  void limitsBookCountWithoutPublishingPartialExports() throws Exception {
    Path file = directory.resolve("count.json");
    assertEquals(
        TransferException.Code.LIMIT,
        assertThrows(
                TransferException.class,
                () ->
                    files.write(
                        Collections.nCopies(JsonCollectionFile.MAX_BOOKS + 1, book),
                        files.inspect(file),
                        false))
            .code());
    assertFalse(Files.exists(file));
    String row = "{\"title\":\"Atlas\",\"author\":\"\",\"location\":\"\",\"note\":\"\"}";
    Files.writeString(
        file,
        "{\"format\":\"bookshelf.collection\",\"version\":1,\"books\":["
            + String.join(",", Collections.nCopies(JsonCollectionFile.MAX_BOOKS + 1, row))
            + "]}");
    assertEquals(
        TransferException.Code.LIMIT,
        assertThrows(TransferException.class, () -> files.read(file)).code());
  }

  @Test
  void rejectsDatabaseTargetsMissingParentsAndDirectories() throws Exception {
    assertThrows(
        TransferException.class, () -> files.inspect(directory.resolve("buecherregal.db")));
    assertThrows(
        TransferException.class, () -> files.inspect(directory.resolve("missing/file.json")));
    Files.createDirectory(directory.resolve("folder.json"));
    assertThrows(TransferException.class, () -> files.inspect(directory.resolve("folder.json")));
  }

  @Test
  void rejectsHardLinksToDatabaseWithoutRequiringPosixAttributes() throws Exception {
    Path database = Files.writeString(directory.resolve("buecherregal.db"), "protected");
    Files.createLink(directory.resolve("hardlink.json"), database);
    assertThrows(TransferException.class, () -> files.inspect(directory.resolve("hardlink.json")));
    assertEquals("protected", Files.readString(database));
  }

  @Test
  void rejectsSymbolicLinksWhenTheTestAccountCanCreateThem() throws Exception {
    Path database = Files.writeString(directory.resolve("buecherregal.db"), "protected");
    try {
      Files.createSymbolicLink(directory.resolve("link.json"), database);
    } catch (IOException | UnsupportedOperationException | SecurityException exception) {
      assumeTrue(false, "Symbolic links unavailable for this test account: " + exception);
    }
    assertThrows(TransferException.class, () -> files.inspect(directory.resolve("link.json")));
    assertEquals("protected", Files.readString(database));
  }
}
