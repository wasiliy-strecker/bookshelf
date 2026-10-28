package de.appfabrik.buecherregal.application;

import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.buecherregal.domain.Book;
import de.appfabrik.buecherregal.domain.BookDraft;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BookServiceTest {
  private final Book first =
      new Book(
          1,
          "Bücher im Frühling",
          "Änne Müller",
          "Wohnzimmer, Regal 2",
          "Geschenk\nvon Ömer. 100% schön_");
  private final Book duplicate = new Book(2, first.title(), "", "", "");
  private final Book other = new Book(3, "Atlas", "Ben", "Flur", "");

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\t\n", "\u2003", "\u00a0"})
  void rejectsMissingOrWhitespaceOnlyTitle(String title) {
    assertThrows(IllegalArgumentException.class, () -> new BookDraft(title, "", "", ""));
  }

  @Test
  void normalizesSingleLineInputButPreservesNoteFormatting() {
    var draft = new BookDraft("  Atlas  ", null, " Flur ", "  Notiz\n\nEnde  ");
    assertEquals("Atlas", draft.title());
    assertEquals("", draft.author());
    assertEquals("Flur", draft.location());
    assertEquals("  Notiz\n\nEnde  ", draft.note());
    assertEquals("", new BookDraft("Atlas", null, null, null).note());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "BÜCHER",
        "äNNE",
        "MÜLLER",
        "WOHNZIMMER",
        "regal 2",
        "öMER",
        "geschenk",
        "100%",
        "schön_",
        "BU\u0308CHER"
      })
  void searchesEveryFieldWithUnicodeCaseHandlingAndLiteralSubstrings(String query) {
    assertEquals(List.of(first), BookService.search(List.of(first, other), query));
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "\n\t"})
  void emptyQueryShowsEntireCollectionSortedByTitleThenId(String query) {
    assertEquals(
        List.of(other, first, duplicate),
        BookService.search(List.of(duplicate, first, other), query));
  }

  @Test
  void preservesDuplicateIdentityAndDoesNotModifyInput() {
    List<Book> input = List.of(duplicate, other, first);
    assertEquals(List.of(first, duplicate), BookService.search(input, "Bücher"));
    assertEquals(List.of(duplicate, other, first), input);
    assertTrue(BookService.search(input, "nicht vorhanden").isEmpty());
  }

  @Test
  void propagatesFailuresWithoutReportingSuccessfulSaveOrDelete() {
    BookService service = new BookService(new FailingRepository());
    BookDraft draft =
        new BookDraft("Unveränderte Eingabe", "Autor", "Ort", "Mehrzeilig\nbleibt erhalten");
    assertThrows(StorageException.class, service::load);
    assertThrows(StorageException.class, () -> service.save(null, draft));
    assertThrows(StorageException.class, () -> service.save(1L, draft));
    assertThrows(StorageException.class, () -> service.delete(1));
    assertEquals("Mehrzeilig\nbleibt erhalten", draft.note());
  }

  private static final class FailingRepository implements BookRepository {
    @Override
    public void initialize() {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public List<Book> findAll() {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public Book create(BookDraft draft) {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public Book update(long id, BookDraft draft) {
      throw new StorageException(StorageException.Code.OPERATION);
    }

    @Override
    public void delete(long id) {
      throw new StorageException(StorageException.Code.OPERATION);
    }
  }
}
