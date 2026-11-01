package de.appfabrik.buecherregal.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NewFilePublisherTest {
  @TempDir Path directory;

  private static final NewFilePublisher.LinkAttempt NO_LINKS =
      (destination, staged) -> {
        throw new UnsupportedOperationException("No hard links on this file system");
      };

  @Test
  void copiesCompleteStagedContentWhenHardLinksAreUnavailable() throws Exception {
    Path staged = Files.writeString(directory.resolve("staged"), "Grüße 東京\nSecond line");
    Path target = directory.resolve("export");
    NewFilePublisher.publish(staged, target, NO_LINKS, Files::copy);
    assertEquals(Files.readString(staged), Files.readString(target));
    assertFalse(Files.isSameFile(staged, target));
  }

  @Test
  void handlesNativeFileSystemErrorsWithoutDependingOnErrorMessageLanguage() throws Exception {
    Path staged = Files.writeString(directory.resolve("staged"), "complete");
    Path target = directory.resolve("export");
    NewFilePublisher.publish(
        staged,
        target,
        (destination, source) -> {
          throw new java.nio.file.FileSystemException(destination.toString(), null, "ENOTSUP");
        },
        Files::copy);
    assertEquals("complete", Files.readString(target));
  }

  @Test
  void preservesAnExistingDestinationAndADestinationCreatedDuringFallback() throws Exception {
    Path staged = Files.writeString(directory.resolve("staged"), "new");
    Path target = Files.writeString(directory.resolve("export"), "original");
    assertThrows(FileAlreadyExistsException.class, () -> NewFilePublisher.publish(staged, target));
    assertThrows(
        FileAlreadyExistsException.class,
        () -> NewFilePublisher.publish(staged, target, NO_LINKS, Files::copy));
    assertEquals("original", Files.readString(target));
    Files.delete(target);
    assertThrows(
        FileAlreadyExistsException.class,
        () ->
            NewFilePublisher.publish(
                staged,
                target,
                (destination, source) -> {
                  Files.writeString(destination, "other application");
                  throw new UnsupportedOperationException("No hard links");
                },
                Files::copy));
    assertEquals("other application", Files.readString(target));
  }

  @Test
  void removesOwnPartialOutputAfterAWriteFailureAndAllowsRetry() throws Exception {
    Path staged = Files.writeString(directory.resolve("staged"), "complete");
    Path target = directory.resolve("export");
    assertThrows(
        IOException.class,
        () ->
            NewFilePublisher.publish(
                staged,
                target,
                NO_LINKS,
                (source, output) -> {
                  output.write(new byte[] {1, 2, 3});
                  throw new IOException("Simulated full disk");
                }));
    assertFalse(Files.exists(target));
    assertEquals("complete", Files.readString(staged));
    NewFilePublisher.publish(staged, target, NO_LINKS, Files::copy);
    assertEquals("complete", Files.readString(target));
  }

  @Test
  void cleanupPreservesAFileThatReplacedOurPartialOutput() throws Exception {
    Path staged = Files.writeString(directory.resolve("staged"), "complete");
    Path target = directory.resolve("export");
    // Keep the displaced inode alive so file identity cannot be reused.
    Path displaced = directory.resolve("displaced");
    assertThrows(
        IOException.class,
        () ->
            NewFilePublisher.publish(
                staged,
                target,
                NO_LINKS,
                (source, output) -> {
                  output.write(42);
                  Files.move(target, displaced);
                  Files.writeString(target, "other application");
                  throw new IOException("Simulated failure after replacement");
                }));
    assertEquals("other application", Files.readString(target));
  }
}
