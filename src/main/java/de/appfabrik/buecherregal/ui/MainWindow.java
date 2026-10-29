package de.appfabrik.buecherregal.ui;

import de.appfabrik.buecherregal.application.BookService;
import de.appfabrik.buecherregal.application.CollectionTransfer;
import de.appfabrik.buecherregal.domain.Book;
import de.appfabrik.buecherregal.i18n.Messages;
import de.appfabrik.buecherregal.persistence.LanguagePreferences;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

final class MainWindow {
  private final Stage stage;
  private final Messages messages;
  private final LanguagePreferences preferences;
  private String languageChoice;
  private final boolean languageReadFailed;
  private final Button language = new Button();
  private final BookService service;
  private final AsyncTasks tasks;
  private final TableView<Book> table = new TableView<>();
  private final TextField search = new TextField();
  private final Label count = new Label();
  private final Label emptyTitle = new Label();
  private final Label emptyHint = new Label();
  private final Label status = new Label();
  private final TextArea note = new TextArea();
  private final Button add = new Button();
  private final Button edit = new Button();
  private final Button delete = new Button();
  private final Button export = new Button();
  private final Button importBooks = new Button();
  private final List<Book> books = new ArrayList<>();
  private boolean busy;
  private boolean ready;
  private boolean loadFailed;
  private BookEditor editor;

  MainWindow(
      Stage stage,
      BookService service,
      CollectionTransfer transfer,
      AsyncTasks tasks,
      Messages messages,
      LanguagePreferences preferences,
      String languageChoice,
      boolean languageReadFailed) {
    this.messages = messages;
    this.preferences = preferences;
    this.languageChoice = languageChoice;
    this.languageReadFailed = languageReadFailed;
    add.setText("+  " + messages.text("book.add"));
    edit.setText(messages.text("action.edit"));
    delete.setText(messages.text("action.delete"));
    export.setText(messages.text("action.export"));
    importBooks.setText(messages.text("action.import"));
    language.setText(messages.text("language.title"));
    language.setId("language-settings");
    language.setOnAction(
        event ->
            new LanguageDialog(
                    stage,
                    messages,
                    tasks,
                    preferences,
                    this.languageChoice,
                    choice -> {
                      this.languageChoice = choice;
                      showStatus(messages.text("language.saved"), false);
                    })
                .show());
    this.stage = stage;
    this.service = service;
    this.tasks = tasks;
    Label heading = new Label("BücherRegal");
    heading.getStyleClass().add("app-heading");
    Label subtitle = new Label(messages.text("app.subtitle"));
    subtitle.getStyleClass().add("muted");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    add.setId("add-book");
    add.getStyleClass().add("primary");
    add.setOnAction(event -> openEditor(null));
    HBox header = new HBox(20, new VBox(4, heading, subtitle), spacer, add);
    header.setAlignment(Pos.CENTER_LEFT);
    UiSupport.textMenu(search, messages);
    UiSupport.textMenu(note, messages);
    note.setId("selected-note");
    status.setId("status-message");
    count.setId("book-count");
    edit.setId("edit-book");
    delete.setId("delete-book");
    export.setId("export-books");
    importBooks.setId("import-books");
    search.setId("search-books");
    search.setPromptText(messages.text("search.prompt"));
    search.setAccessibleText(messages.text("search.accessible"));
    search.textProperty().addListener((observable, previous, current) -> refresh(null));
    HBox.setHgrow(search, Priority.ALWAYS);
    Button clear = new Button(messages.text("search.clear"));
    clear.setId("clear-search");
    clear.setOnAction(
        event -> {
          search.clear();
          search.requestFocus();
        });
    clear.disableProperty().bind(search.textProperty().isEmpty());
    HBox searchBar = new HBox(10, search, clear);
    table.setId("books-table");
    table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    table.getColumns().add(column(messages.text("field.title"), Book::title, 360));
    table.getColumns().add(column(messages.text("field.author"), Book::author, 220));
    table.getColumns().add(column(messages.text("field.location"), Book::location, 240));
    table
        .getSelectionModel()
        .selectedItemProperty()
        .addListener(
            (observable, previous, selected) -> {
              note.setText(selected == null ? "" : selected.note());
              note.setPromptText(
                  selected == null ? messages.text("note.select") : messages.text("note.empty"));
              updateButtons();
            });
    table.setOnMouseClicked(
        event -> {
          if (event.getClickCount() == 2 && event.getTarget() instanceof javafx.scene.Node node) {
            while (node != null && !(node instanceof javafx.scene.control.TableRow<?>)) {
              node = node.getParent();
            }
            if (node instanceof javafx.scene.control.TableRow<?> row && !row.isEmpty()) {
              editSelected();
            }
          }
        });
    table.setOnKeyPressed(
        event -> {
          if (event.getCode() == KeyCode.ENTER) {
            editSelected();
            event.consume();
          }
          if (event.getCode() == KeyCode.DELETE) {
            deleteSelected();
            event.consume();
          }
        });
    emptyTitle.getStyleClass().add("section-heading");
    emptyHint.getStyleClass().add("muted");
    emptyHint.setWrapText(true);
    emptyHint.setMaxWidth(470);
    VBox placeholder = new VBox(12, emptyTitle, emptyHint);
    placeholder.setAlignment(Pos.CENTER);
    placeholder.setPadding(new Insets(30));
    table.setPlaceholder(placeholder);
    count.getStyleClass().add("muted");
    edit.setOnAction(event -> editSelected());
    delete.setOnAction(event -> deleteSelected());
    delete.getStyleClass().add("danger");
    Region actionsSpacer = new Region();
    HBox.setHgrow(actionsSpacer, Priority.ALWAYS);
    HBox actions = new HBox(10, count, actionsSpacer, edit, delete);
    actions.setAlignment(Pos.CENTER_LEFT);
    Label noteHeading = new Label(messages.text("field.note"));
    noteHeading.getStyleClass().add("field-label");
    note.setEditable(false);
    note.setWrapText(true);
    note.setPrefRowCount(3);
    note.setPromptText(messages.text("note.select"));
    note.setAccessibleText(messages.text("note.accessible"));
    status.setWrapText(true);
    status.setMinHeight(Region.USE_PREF_SIZE);
    status.getStyleClass().add("muted");
    status.setMaxWidth(Double.MAX_VALUE);
    status.setMinWidth(0);
    HBox.setHgrow(status, Priority.ALWAYS);
    var transfers =
        new CollectionActions(
            stage,
            transfer,
            tasks,
            messages,
            this::setBusy,
            this::showStatus,
            result -> {
              books.clear();
              books.addAll(result.books());
              search.clear();
              refresh(null);
            });
    export.setOnAction(event -> transfers.exportCollection());
    importBooks.setOnAction(event -> transfers.importCollection());
    export.setMinWidth(Region.USE_PREF_SIZE);
    importBooks.setMinWidth(Region.USE_PREF_SIZE);
    language.setMinWidth(Region.USE_PREF_SIZE);
    HBox footer = new HBox(12, status, language, export, importBooks);
    footer.setAlignment(Pos.CENTER_LEFT);
    VBox root =
        new VBox(18, header, searchBar, table, actions, new VBox(6, noteHeading, note), footer);
    root.setPadding(new Insets(30));
    VBox.setVgrow(table, Priority.ALWAYS);
    Scene scene = new Scene(root, 980, 760);
    UiSupport.style(scene);
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
            () -> openEditor(null));
    scene
        .getAccelerators()
        .put(
            new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
            () -> {
              search.requestFocus();
              search.selectAll();
            });
    stage.setTitle("BücherRegal");
    UiSupport.icon(stage);
    stage.setMinWidth(760);
    stage.setMinHeight(620);
    stage.setScene(scene);
    stage.setOnCloseRequest(
        event -> {
          if (busy) {
            event.consume();
          } else if (editor != null && editor.isShowing()) {
            event.consume();
            editor.requestClose();
          }
        });
  }

  private static TableColumn<Book, String> column(
      String title, Function<Book, String> value, int width) {
    TableColumn<Book, String> column = new TableColumn<>(title);
    column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(value.apply(cell.getValue())));
    column.setPrefWidth(width);
    column.setSortable(false);
    return column;
  }

  void show() {
    stage.show();
    load();
  }

  private void load() {
    if (busy) {
      return;
    }
    setBusy(true);
    loadFailed = false;
    showStatus(messages.text("loading"), false);
    refresh(null);
    tasks.submit(
        service::load,
        loaded -> {
          books.clear();
          books.addAll(loaded);
          ready = true;
          setBusy(false);
          refresh(null);
          showStatus(
              messages.text(languageReadFailed ? "language.loadError" : "stored"),
              languageReadFailed);
        },
        failure -> {
          loadFailed = true;
          setBusy(false);
          refresh(null);
          showStatus(UiSupport.message(failure, messages), true);
        });
  }

  private void refresh(Long preferredId) {
    Book selected = table.getSelectionModel().getSelectedItem();
    Long selection = preferredId != null ? preferredId : selected == null ? null : selected.id();
    List<Book> matches = BookService.search(books, search.getText(), messages.locale());
    table.getItems().setAll(matches);
    if (selection != null) {
      matches.stream()
          .filter(book -> book.id() == selection)
          .findFirst()
          .ifPresent(
              book -> {
                table.getSelectionModel().select(book);
                if (preferredId != null) {
                  table.scrollTo(book);
                }
              });
    }
    count.setText(
        !ready ? "" : messages.count(matches.size(), books.size(), !search.getText().isBlank()));
    if (loadFailed) {
      emptyTitle.setText(messages.text("empty.failed"));
      emptyHint.setText(messages.text("empty.failedHint"));
    } else if (!ready) {
      emptyTitle.setText(messages.text("loading"));
      emptyHint.setText("");
    } else if (books.isEmpty()) {
      emptyTitle.setText(messages.text("empty.title"));
      emptyHint.setText(messages.text("empty.hint"));
    } else {
      emptyTitle.setText(messages.text("empty.search"));
      emptyHint.setText(messages.text("empty.searchHint"));
    }
    updateButtons();
  }

  private void openEditor(Book book) {
    if (!ready || busy || loadFailed || (editor != null && editor.isShowing())) {
      return;
    }
    editor =
        new BookEditor(
            stage,
            book,
            service,
            tasks,
            messages,
            saved -> {
              books.removeIf(existing -> existing.id() == saved.id());
              books.add(saved);
              refresh(saved.id());
              showStatus(
                  BookService.search(List.of(saved), search.getText(), messages.locale()).isEmpty()
                      ? messages.text("saved.filtered")
                      : messages.text("stored"),
                  false);
            });
    editor.show();
  }

  private void editSelected() {
    Book selected = table.getSelectionModel().getSelectedItem();
    if (selected != null) {
      openEditor(selected);
    }
  }

  private void deleteSelected() {
    Book selected = table.getSelectionModel().getSelectedItem();
    if (selected == null || busy || loadFailed) {
      return;
    }
    if (!UiSupport.confirm(
        stage,
        messages.text("delete.title"),
        messages.text("delete.detail", selected.title()),
        messages.text("action.delete"),
        messages.text("action.cancel"))) {
      return;
    }
    setBusy(true);
    showStatus(messages.text("delete.busy"), false);
    tasks.submit(
        () -> {
          service.delete(selected.id());
          return selected;
        },
        deleted -> {
          books.removeIf(book -> book.id() == deleted.id());
          setBusy(false);
          refresh(null);
          showStatus(messages.text("stored"), false);
        },
        failure -> {
          setBusy(false);
          showStatus(UiSupport.message(failure, messages), true);
        });
  }

  private void setBusy(boolean value) {
    busy = value;
    table.setDisable(value);
    language.setDisable(value);
    updateButtons();
  }

  private void updateButtons() {
    add.setDisable(busy || !ready || loadFailed);
    export.setDisable(busy || !ready || loadFailed);
    importBooks.setDisable(busy || !ready || loadFailed);
    boolean unavailable = busy || loadFailed || table.getSelectionModel().getSelectedItem() == null;
    edit.setDisable(unavailable);
    delete.setDisable(unavailable);
  }

  private void showStatus(String text, boolean error) {
    status.setText(text);
    status.getStyleClass().removeAll("muted", "error-text");
    status.getStyleClass().add(error ? "error-text" : "muted");
  }
}
