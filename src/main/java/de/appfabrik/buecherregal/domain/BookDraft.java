package de.appfabrik.buecherregal.domain;

/** Validated input shared by creation and editing. Notes retain their formatting. */
public record BookDraft(String title, String author, String location, String note) {
  public BookDraft {
    title = title == null ? "" : title.strip();
    if (title
        .codePoints()
        .noneMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))) {
      throw new IllegalArgumentException("Bitte gib einen Buchtitel ein.");
    }
    author = author == null ? "" : author.strip();
    location = location == null ? "" : location.strip();
    note = note == null ? "" : note;
  }

  public Book withId(long id) {
    return new Book(id, title, author, location, note);
  }
}
