package de.appfabrik.buecherregal.application;

import de.appfabrik.buecherregal.domain.Book;
import java.util.List;

/** The committed collection and the number of imported or already present copies. */
public record ImportResult(int added, int skipped, List<Book> books) {
  public ImportResult {
    books = List.copyOf(books);
  }
}
