package de.appfabrik.buecherregal.application;

import de.appfabrik.buecherregal.domain.Book;
import de.appfabrik.buecherregal.domain.BookDraft;
import java.text.Collator;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Small application layer, independent of JavaFX and JDBC. */
public final class BookService {
  private final BookRepository repository;

  public BookService(BookRepository repository) {
    this.repository = repository;
  }

  public List<Book> load() {
    repository.initialize();
    return repository.findAll();
  }

  public Book save(Long id, BookDraft draft) {
    return id == null ? repository.create(draft) : repository.update(id, draft);
  }

  public void delete(long id) {
    repository.delete(id);
  }

  public static List<Book> search(List<Book> books, String query) {
    return search(books, query, Locale.GERMAN);
  }

  public static List<Book> search(List<Book> books, String query, Locale locale) {
    String needle = normalize(query == null ? "" : query.strip());
    Collator collator = Collator.getInstance(locale);
    collator.setStrength(Collator.SECONDARY);
    return books.stream()
        .filter(book -> matches(book, needle))
        .sorted(Comparator.comparing(Book::title, collator).thenComparingLong(Book::id))
        .toList();
  }

  private static boolean matches(Book book, String needle) {
    return normalize(book.title()).contains(needle)
        || normalize(book.author()).contains(needle)
        || normalize(book.location()).contains(needle)
        || normalize(book.note()).contains(needle);
  }

  private static String normalize(String text) {
    return Normalizer.normalize(text, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
  }
}
