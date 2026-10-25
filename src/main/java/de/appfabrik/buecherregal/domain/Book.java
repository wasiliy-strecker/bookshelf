package de.appfabrik.buecherregal.domain;

/** A persisted book. Identity is independent of title and author. */
public record Book(long id, String title, String author, String location, String note) {
  public BookDraft draft() {
    return new BookDraft(title, author, location, note);
  }
}
