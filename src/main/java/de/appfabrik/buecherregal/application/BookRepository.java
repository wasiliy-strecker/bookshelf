package de.appfabrik.buecherregal.application;

import de.appfabrik.buecherregal.domain.Book;
import de.appfabrik.buecherregal.domain.BookDraft;
import java.util.List;

/** Storage port. Methods complete only after the database operation succeeds. */
public interface BookRepository {
  void initialize();

  List<Book> findAll();

  Book create(BookDraft draft);

  Book update(long id, BookDraft draft);

  void delete(long id);
}
