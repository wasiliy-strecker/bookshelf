package de.appfabrik.buecherregal.persistence;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

/** Publishes staged content without replacing a concurrently created destination. */
final class NewFilePublisher {
  private NewFilePublisher() {}

  static void publish(Path staged, Path destination) throws IOException {
    publish(staged, destination, Files::createLink, Files::copy);
  }

  static void publish(Path staged, Path destination, LinkAttempt link, CopyContent copy)
      throws IOException {
    Exception linkFailure;
    try {
      link.create(destination, staged);
      return;
    } catch (FileAlreadyExistsException exists) {
      throw exists;
    } catch (IOException | UnsupportedOperationException unavailable) {
      // exFAT and some file providers cannot create hard links. CREATE_NEW still
      // reserves the destination atomically, including against symbolic links.
      linkFailure = unavailable;
    }
    Object createdKey = null;
    try (OutputStream output =
        Files.newOutputStream(
            destination, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
      createdKey =
          Files.readAttributes(destination, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)
              .fileKey();
      copy.write(staged, output);
    } catch (IOException | RuntimeException failure) {
      failure.addSuppressed(linkFailure);
      // Never delete a destination another process has replaced. On providers
      // without file identity, retain the failed output rather than risk user data.
      if (createdKey != null) {
        try {
          var current =
              Files.readAttributes(
                  destination, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
          if (current.isRegularFile() && createdKey.equals(current.fileKey())) {
            Files.delete(destination);
          }
        } catch (IOException | RuntimeException cleanup) {
          failure.addSuppressed(cleanup);
        }
      }
      throw failure;
    }
  }

  @FunctionalInterface
  interface LinkAttempt {
    void create(Path destination, Path staged) throws IOException;
  }

  @FunctionalInterface
  interface CopyContent {
    void write(Path staged, OutputStream output) throws IOException;
  }
}
