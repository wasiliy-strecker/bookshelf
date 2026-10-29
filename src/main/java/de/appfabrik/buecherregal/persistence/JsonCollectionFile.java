package de.appfabrik.buecherregal.persistence;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import de.appfabrik.buecherregal.application.CollectionFile;
import de.appfabrik.buecherregal.application.CollectionTarget;
import de.appfabrik.buecherregal.application.TransferException;
import de.appfabrik.buecherregal.domain.BookDraft;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Versioned UTF-8 JSON with strict validation and atomic publication. */
public final class JsonCollectionFile implements CollectionFile {
  public static final int MAX_BYTES = 10 * 1024 * 1024;
  public static final int MAX_BOOKS = 10_000;
  private static final Set<String> FIELDS = Set.of("title", "author", "location", "note");
  private final Path dataDirectory;

  public JsonCollectionFile(Path dataDirectory) {
    this.dataDirectory = dataDirectory.toAbsolutePath().normalize();
  }

  @Override
  public CollectionTarget inspect(Path selected) {
    try {
      Path absolute = selected.toAbsolutePath().normalize();
      Path parent = absolute.getParent();
      Path filename = absolute.getFileName();
      if (parent == null
          || filename == null
          || !filename.toString().toLowerCase(Locale.ROOT).endsWith(".json")) {
        throw new TransferException(TransferException.Code.TARGET);
      }
      Path path = parent.toRealPath().resolve(filename);
      for (String name :
          List.of(
              "buecherregal.db",
              "buecherregal.db-wal",
              "buecherregal.db-shm",
              "buecherregal.db-journal",
              "language.txt")) {
        Path protectedFile = dataDirectory.resolve(name);
        if (Files.exists(path)
            && Files.exists(protectedFile)
            && Files.isSameFile(path, protectedFile)) {
          throw new TransferException(TransferException.Code.TARGET);
        }
      }
      try {
        var attributes =
            Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (!attributes.isRegularFile()) {
          throw new TransferException(TransferException.Code.TARGET);
        }
        return new CollectionTarget(
            path,
            true,
            attributes.fileKey(),
            attributes.size(),
            attributes.lastModifiedTime().toInstant());
      } catch (NoSuchFileException exception) {
        return new CollectionTarget(path, false, null, 0, null);
      }
    } catch (IOException | SecurityException exception) {
      throw new TransferException(TransferException.Code.TARGET, exception);
    }
  }

  @Override
  public List<BookDraft> read(Path path) {
    byte[] bytes;
    try (var input = Files.newInputStream(path)) {
      bytes = input.readNBytes(MAX_BYTES + 1);
    } catch (IOException | SecurityException exception) {
      throw new TransferException(TransferException.Code.READ, exception);
    }
    if (bytes.length > MAX_BYTES) {
      throw new TransferException(TransferException.Code.LIMIT);
    }
    try {
      String text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
      try (var reader = new JsonReader(new StringReader(text))) {
        reader.setStrictness(Strictness.STRICT);
        reader.beginObject();
        Set<String> keys = new HashSet<>();
        List<BookDraft> books = null;
        while (reader.hasNext()) {
          String key = reader.nextName();
          require(keys.add(key));
          switch (key) {
            case "format" -> require(readText(reader).equals("bookshelf.collection"));
            case "version" -> {
              require(reader.peek() == JsonToken.NUMBER);
              if (!reader.nextString().equals("1")) {
                throw new TransferException(TransferException.Code.VERSION);
              }
            }
            case "books" -> books = readBooks(reader);
            default -> throw new TransferException(TransferException.Code.FORMAT);
          }
        }
        reader.endObject();
        require(keys.equals(Set.of("format", "version", "books")) && books != null);
        require(reader.peek() == JsonToken.END_DOCUMENT);
        return List.copyOf(books);
      }
    } catch (IOException | IllegalStateException | IllegalArgumentException exception) {
      throw new TransferException(TransferException.Code.FORMAT, exception);
    }
  }

  private static List<BookDraft> readBooks(JsonReader reader) throws IOException {
    reader.beginArray();
    List<BookDraft> result = new ArrayList<>();
    while (reader.hasNext()) {
      if (result.size() >= MAX_BOOKS) {
        throw new TransferException(TransferException.Code.LIMIT);
      }
      reader.beginObject();
      var values = new HashMap<String, String>();
      while (reader.hasNext()) {
        String key = reader.nextName();
        require(FIELDS.contains(key) && !values.containsKey(key));
        values.put(key, readText(reader));
      }
      reader.endObject();
      require(values.keySet().equals(FIELDS));
      result.add(
          new BookDraft(
              values.get("title"),
              values.get("author"),
              values.get("location"),
              values.get("note")));
    }
    reader.endArray();
    return result;
  }

  private static String readText(JsonReader reader) throws IOException {
    require(reader.peek() == JsonToken.STRING);
    String value = reader.nextString();
    validateUnicode(value);
    return value;
  }

  private static void validateUnicode(String text) {
    for (int i = 0; i < text.length(); i++) {
      char character = text.charAt(i);
      if (Character.isHighSurrogate(character)) {
        require(i + 1 < text.length() && Character.isLowSurrogate(text.charAt(++i)));
      } else {
        require(!Character.isLowSurrogate(character));
      }
    }
  }

  private static void require(boolean valid) {
    if (!valid) {
      throw new TransferException(TransferException.Code.FORMAT);
    }
  }

  @Override
  public void write(List<BookDraft> books, CollectionTarget target, boolean replacementConfirmed) {
    if (books.size() > MAX_BOOKS) {
      throw new TransferException(TransferException.Code.LIMIT);
    }
    if (target.exists() && !replacementConfirmed) {
      throw new TransferException(TransferException.Code.CONFIRM);
    }
    Path temporary = null;
    try {
      if (!target.equals(inspect(target.path()))) {
        throw new TransferException(TransferException.Code.CHANGED);
      }
      Path parent = target.path().getParent();
      if (parent == null) {
        throw new TransferException(TransferException.Code.TARGET);
      }
      temporary = Files.createTempFile(parent, ".bookshelf-", ".json.tmp");
      try (var bytes = new LimitedOutput(Files.newOutputStream(temporary));
          var writer = new JsonWriter(new OutputStreamWriter(bytes, StandardCharsets.UTF_8))) {
        writer.setStrictness(Strictness.STRICT);
        writer.setIndent("  ");
        writer.beginObject();
        writer.name("format").value("bookshelf.collection");
        writer.name("version").value(1);
        writer.name("books").beginArray();
        for (BookDraft book : books) {
          writer.beginObject();
          for (String value : List.of(book.title(), book.author(), book.location(), book.note())) {
            validateUnicode(value);
          }
          writer.name("title").value(book.title());
          writer.name("author").value(book.author());
          writer.name("location").value(book.location());
          writer.name("note").value(book.note());
          writer.endObject();
        }
        writer.endArray();
        writer.endObject();
      }
      if (!target.equals(inspect(target.path()))) {
        throw new TransferException(TransferException.Code.CHANGED);
      }
      if (target.exists()) {
        // Unsupported atomic replacement leaves the existing export untouched.
        Files.move(
            temporary,
            target.path(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING);
      } else {
        // Publish without overwriting a concurrently created target.
        Files.createLink(target.path(), temporary);
      }
    } catch (IOException | SecurityException | UnsupportedOperationException exception) {
      throw new TransferException(TransferException.Code.WRITE, exception);
    } finally {
      if (temporary != null) {
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException | SecurityException ignored) {
          // A cleanup failure must not change the committed result.
        }
      }
    }
  }

  private static final class LimitedOutput extends OutputStream {
    private final OutputStream delegate;
    private int count;

    private LimitedOutput(OutputStream delegate) {
      this.delegate = delegate;
    }

    @Override
    public void write(int value) throws IOException {
      reserve(1);
      delegate.write(value);
    }

    @Override
    public void write(byte[] bytes, int offset, int length) throws IOException {
      reserve(length);
      delegate.write(bytes, offset, length);
    }

    private void reserve(int length) {
      if (length > MAX_BYTES - count) {
        throw new TransferException(TransferException.Code.LIMIT);
      }
      count += length;
    }

    @Override
    public void close() throws IOException {
      delegate.close();
    }
  }
}
