package de.appfabrik.buecherregal.application;

import de.appfabrik.buecherregal.domain.Book;
import de.appfabrik.buecherregal.domain.BookDraft;
import java.nio.file.Path;
import java.util.List;

/** Always exports the saved collection, independent of selection and search. */
public final class CollectionTransfer {
  private final BookRepository repository;
  private final CollectionFile files;

  public CollectionTransfer(BookRepository repository, CollectionFile files) {
    this.repository = repository;
    this.files = files;
  }

  public CollectionTarget inspect(Path path) {
    return files.inspect(path);
  }

  public int exportBooks(CollectionTarget target, boolean replacementConfirmed) {
    List<BookDraft> books = repository.findAll().stream().map(Book::draft).toList();
    files.write(books, target, replacementConfirmed);
    return books.size();
  }

  public List<BookDraft> readImport(Path path) {
    return files.read(path);
  }

  public ImportResult importBooks(List<BookDraft> books) {
    return repository.importBooks(books);
  }
}
