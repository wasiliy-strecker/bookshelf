package de.appfabrik.buecherregal.i18n;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PrivacyNoticeTest {
  @TempDir Path directory;

  @Test
  void exportedNoticesMatchTheOfflineDialogInEveryLanguageAndCannotBeOverwritten()
      throws Exception {
    Path output = directory.resolve("privacy");
    PrivacyTextExport.export(output);
    for (AppLanguage language : AppLanguage.values()) {
      String text = Files.readString(output.resolve(language.tag() + ".txt"));
      assertEquals(PrivacyNotice.text(new Messages(language)), text);
      assertTrue(text.length() > 2500);
      assertTrue(text.contains("Wasiliy Strecker"));
      assertTrue(text.contains("contact@appfabrik-ai.de"));
      assertTrue(text.contains("support@appfabrik-ai.de"));
      assertTrue(text.contains("JSON"));
      assertFalse(text.contains("{0}"));
    }
    assertThrows(FileAlreadyExistsException.class, () -> PrivacyTextExport.export(output));
  }
}
