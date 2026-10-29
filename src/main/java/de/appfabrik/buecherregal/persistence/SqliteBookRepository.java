package de.appfabrik.buecherregal.persistence;

import de.appfabrik.buecherregal.application.BookRepository;
import de.appfabrik.buecherregal.application.ImportResult;
import de.appfabrik.buecherregal.application.StorageException;
import de.appfabrik.buecherregal.application.TransferException;
import de.appfabrik.buecherregal.domain.Book;
import de.appfabrik.buecherregal.domain.BookDraft;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.sqlite.SQLiteConfig;

/** Opens and closes one JDBC connection per operation. No connection escapes this class. */
public final class SqliteBookRepository implements BookRepository {
  private final Path database;

  public SqliteBookRepository(Path database) {
    this.database = database.toAbsolutePath().normalize();
  }

  private Connection connect() throws SQLException {
    SQLiteConfig config = new SQLiteConfig();
    config.setBusyTimeout(2000);
    return config.createConnection("jdbc:sqlite:" + database);
  }

  @Override
  public void initialize() {
    try {
      Path parent = database.getParent();
      if (parent == null) {
        throw new StorageException(StorageException.Code.PATH);
      }
      Files.createDirectories(parent);
      try (Connection connection = connect()) {
        connection.setAutoCommit(false);
        try (var statement = connection.createStatement()) {
          int version;
          try (var result = statement.executeQuery("PRAGMA user_version")) {
            result.next();
            version = result.getInt(1);
          }
          if (version > 1) {
            throw new StorageException(StorageException.Code.NEWER_SCHEMA);
          }
          if (version == 0) {
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS books (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  title TEXT NOT NULL CHECK (length(trim(title)) > 0),
                  author TEXT NOT NULL DEFAULT '',
                  location TEXT NOT NULL DEFAULT '',
                  note TEXT NOT NULL DEFAULT ''
                )
                """);
            statement.executeUpdate("PRAGMA user_version = 1");
          }
          connection.commit();
        } catch (SQLException | RuntimeException exception) {
          connection.rollback();
          throw exception;
        }
      }
    } catch (IOException | SQLException exception) {
      throw new StorageException(StorageException.Code.OPEN, exception);
    }
  }

  @Override
  public List<Book> findAll() {
    try (Connection connection = connect()) {
      return readBooks(connection);
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  private static List<Book> readBooks(Connection connection) throws SQLException {
    try (var statement = connection.createStatement();
        var result =
            statement.executeQuery(
                "SELECT id, title, author, location, note FROM books ORDER BY id")) {
      List<Book> books = new ArrayList<>();
      while (result.next()) {
        books.add(
            new Book(
                result.getLong("id"),
                result.getString("title"),
                result.getString("author"),
                result.getString("location"),
                result.getString("note")));
      }
      return List.copyOf(books);
    }
  }

  @Override
  public ImportResult importBooks(List<BookDraft> drafts) {
    List<BookDraft> input = List.copyOf(drafts);
    try (Connection connection = connect()) {
      connection.setAutoCommit(false);
      try {
        var available = new HashMap<BookDraft, Integer>();
        for (Book book : readBooks(connection)) {
          available.merge(book.draft(), 1, Integer::sum);
        }
        int added = 0;
        int skipped = 0;
        try (var statement =
            connection.prepareStatement(
                "INSERT INTO books (title, author, location, note) VALUES (?, ?, ?, ?)")) {
          for (BookDraft draft : input) {
            int copies = available.getOrDefault(draft, 0);
            if (copies > 0) {
              available.put(draft, copies - 1);
              skipped++;
            } else {
              bind(statement, draft);
              statement.executeUpdate();
              added++;
            }
          }
        }
        var result = new ImportResult(added, skipped, readBooks(connection));
        connection.commit();
        return result;
      } catch (SQLException | RuntimeException exception) {
        connection.rollback();
        throw exception;
      }
    } catch (SQLException exception) {
      throw new TransferException(TransferException.Code.IMPORT, exception);
    }
  }

  @Override
  public Book create(BookDraft draft) {
    try (Connection connection = connect();
        PreparedStatement statement =
            connection.prepareStatement(
                "INSERT INTO books (title, author, location, note) VALUES (?, ?, ?, ?) RETURNING id")) {
      bind(statement, draft);
      try (ResultSet result = statement.executeQuery()) {
        if (!result.next()) {
          throw new StorageException(StorageException.Code.CREATE);
        }
        return draft.withId(result.getLong(1));
      }
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  @Override
  public Book update(long id, BookDraft draft) {
    try (Connection connection = connect();
        PreparedStatement statement =
            connection.prepareStatement(
                "UPDATE books SET title = ?, author = ?, location = ?, note = ? WHERE id = ?")) {
      bind(statement, draft);
      statement.setLong(5, id);
      requireExisting(statement.executeUpdate());
      return draft.withId(id);
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  @Override
  public void delete(long id) {
    try (Connection connection = connect();
        PreparedStatement statement =
            connection.prepareStatement("DELETE FROM books WHERE id = ?")) {
      statement.setLong(1, id);
      requireExisting(statement.executeUpdate());
    } catch (SQLException exception) {
      throw failure(exception);
    }
  }

  private static void bind(PreparedStatement statement, BookDraft draft) throws SQLException {
    statement.setString(1, draft.title());
    statement.setString(2, draft.author());
    statement.setString(3, draft.location());
    statement.setString(4, draft.note());
  }

  private static void requireExisting(int affected) {
    if (affected != 1) {
      throw new StorageException(StorageException.Code.MISSING);
    }
  }

  private static StorageException failure(SQLException exception) {
    return new StorageException(StorageException.Code.OPERATION, exception);
  }
}
