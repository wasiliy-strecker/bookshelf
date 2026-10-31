package de.appfabrik.buecherregal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExportFilenameTest {
  @TempDir Path folder;

  @Test
  void preservesUnicodeWithinTheGrantedFolder() {
    assertEquals(folder.resolve("Bücher 日本.json"), ExportFilename.resolve(folder, " Bücher 日本 "));
    assertEquals(folder.resolve("Books.JSON"), ExportFilename.resolve(folder, "Books.JSON"));
  }

  @Test
  void refusesTraversalAbsolutePathsAndInvalidNames() {
    for (String name :
        List.of(
            "",
            " ",
            ".",
            "..",
            "../books",
            "/tmp/books",
            "x/y",
            "x\\y",
            "C:books",
            "a\nb",
            "x".repeat(251),
            "書".repeat(84))) {
      assertThrows(
          IllegalArgumentException.class, () -> ExportFilename.resolve(folder, name), name);
    }
  }
}
