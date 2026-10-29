package de.appfabrik.buecherregal.ui;

import de.appfabrik.buecherregal.application.CollectionTarget;
import de.appfabrik.buecherregal.application.CollectionTransfer;
import de.appfabrik.buecherregal.application.ImportResult;
import de.appfabrik.buecherregal.i18n.Messages;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** Native file selection stays on the UI thread. File and database work stays on the worker. */
final class CollectionActions {
  private final Stage owner;
  private final CollectionTransfer transfer;
  private final AsyncTasks tasks;
  private final Messages messages;
  private final Consumer<Boolean> busy;
  private final BiConsumer<String, Boolean> status;
  private final Consumer<ImportResult> imported;
  private boolean running;

  CollectionActions(
      Stage owner,
      CollectionTransfer transfer,
      AsyncTasks tasks,
      Messages messages,
      Consumer<Boolean> busy,
      BiConsumer<String, Boolean> status,
      Consumer<ImportResult> imported) {
    this.owner = owner;
    this.transfer = transfer;
    this.tasks = tasks;
    this.messages = messages;
    this.busy = busy;
    this.status = status;
    this.imported = imported;
  }

  void exportCollection() {
    if (!begin()) {
      return;
    }
    try {
      var chooser = chooser("transfer.exportTitle");
      chooser.setInitialFileName("buecherregal-" + LocalDate.now() + ".json");
      File selected = chooser.showSaveDialog(owner);
      if (selected == null) {
        finish("transfer.cancelled");
        return;
      }
      Path path = selected.toPath();
      Path filename = Objects.requireNonNull(path.getFileName());
      if (!filename.toString().contains(".")) {
        path = path.resolveSibling(filename + ".json");
      }
      Path target = path;
      tasks.submit(() -> transfer.inspect(target), this::exportTo, this::failed);
    } catch (RuntimeException exception) {
      failed(exception);
    }
  }

  private void exportTo(CollectionTarget target) {
    if (target.exists()
        && !UiSupport.confirm(
            owner,
            messages.text("transfer.replaceTitle"),
            messages.text("transfer.replaceDetail", target.path().getFileName().toString()),
            messages.text("transfer.replace"),
            messages.text("action.cancel"))) {
      finish("transfer.cancelled");
      return;
    }
    status.accept(messages.text("transfer.exportBusy"), false);
    tasks.submit(
        () -> transfer.exportBooks(target, target.exists()),
        count -> finish("transfer.exported", count),
        this::failed);
  }

  void importCollection() {
    if (!begin()) {
      return;
    }
    try {
      File selected = chooser("transfer.importTitle").showOpenDialog(owner);
      if (selected == null) {
        finish("transfer.cancelled");
        return;
      }
      status.accept(messages.text("transfer.readBusy"), false);
      tasks.submit(
          () -> transfer.readImport(selected.toPath()),
          books -> {
            if (!UiSupport.confirm(
                owner,
                messages.text("transfer.confirmTitle"),
                messages.text("transfer.confirmDetail", books.size()),
                messages.text("action.import"),
                messages.text("action.cancel"))) {
              finish("transfer.cancelled");
              return;
            }
            status.accept(messages.text("transfer.importBusy"), false);
            tasks.submit(
                () -> transfer.importBooks(books),
                result -> {
                  imported.accept(result);
                  finish("transfer.imported", result.added(), result.skipped());
                },
                this::failed);
          },
          this::failed);
    } catch (RuntimeException exception) {
      failed(exception);
    }
  }

  private FileChooser chooser(String title) {
    var chooser = new FileChooser();
    chooser.setTitle(messages.text(title));
    chooser
        .getExtensionFilters()
        .add(new FileChooser.ExtensionFilter(messages.text("transfer.filter"), "*.json", "*.JSON"));
    return chooser;
  }

  private boolean begin() {
    if (running) {
      return false;
    }
    running = true;
    busy.accept(true);
    return true;
  }

  private void finish(String key, Object... arguments) {
    running = false;
    busy.accept(false);
    status.accept(messages.text(key, arguments), false);
  }

  private void failed(Throwable failure) {
    running = false;
    busy.accept(false);
    status.accept(UiSupport.message(failure, messages), true);
  }
}
