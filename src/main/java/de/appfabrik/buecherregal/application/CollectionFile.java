package de.appfabrik.buecherregal.application;

import de.appfabrik.buecherregal.domain.BookDraft;
import java.nio.file.Path;
import java.util.List;

/** Portable collection file port. Reading validates the entire document before returning. */
public interface CollectionFile {
  CollectionTarget inspect(Path path);

  List<BookDraft> read(Path path);

  void write(List<BookDraft> books, CollectionTarget target, boolean replacementConfirmed);
}
