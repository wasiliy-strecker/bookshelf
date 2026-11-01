package de.appfabrik.buecherregal.ui;

import de.appfabrik.buecherregal.application.BookService;
import de.appfabrik.buecherregal.domain.Book;
import de.appfabrik.buecherregal.domain.BookDraft;
import de.appfabrik.buecherregal.i18n.Messages;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

final class BookEditor {
  private final Messages messages;
  private final Stage stage = new Stage();
  private final TextField title = new TextField();
  private final TextField author = new TextField();
  private final TextField location = new TextField();
  private final TextArea note = new TextArea();
  private final Label validation = new Label();
  private final Label error = new Label();
  private final Button save = new Button();
  private final Button cancel = new Button();
  private final BookService service;
  private final AsyncTasks tasks;
  private final Long id;
  private final Input original;
  private final Consumer<Book> onSaved;
  private boolean saving;
  private final CloseRequests closeRequests = new CloseRequests(this::closeIfApproved);

  BookEditor(
      Window owner,
      Book existing,
      BookService service,
      AsyncTasks tasks,
      Messages messages,
      Consumer<Book> onSaved) {
    this.messages = messages;
    save.setText(messages.text("action.save"));
    cancel.setText(messages.text("action.cancel"));
    cancel.setId("cancel-book");
    validation.setId("title-validation");
    validation.setWrapText(true);
    error.setId("save-error");
    UiSupport.icon(stage);
    UiSupport.textMenu(title, messages);
    UiSupport.textMenu(author, messages);
    UiSupport.textMenu(location, messages);
    UiSupport.textMenu(note, messages);
    this.service = service;
    this.tasks = tasks;
    this.onSaved = onSaved;
    this.id = existing == null ? null : existing.id();
    if (existing != null) {
      title.setText(existing.title());
      author.setText(existing.author());
      location.setText(existing.location());
      note.setText(existing.note());
    }
    original = input();
    stage.initOwner(owner);
    stage.initModality(Modality.WINDOW_MODAL);
    stage.setTitle(existing == null ? messages.text("book.add") : messages.text("book.edit"));
    stage.setMinWidth(480);
    stage.setMinHeight(590);
    Label heading = new Label(stage.getTitle());
    heading.getStyleClass().add("section-heading");
    Label hint = new Label(messages.text("editor.hint"));
    hint.getStyleClass().add("muted");
    hint.setWrapText(true);
    title.setId("book-title");
    author.setId("book-author");
    location.setId("book-location");
    note.setId("book-note");
    title.setPromptText(messages.text("editor.titlePrompt"));
    author.setPromptText(messages.text("field.optional"));
    location.setPromptText(messages.text("editor.locationPrompt"));
    note.setPromptText(messages.text("editor.notePrompt"));
    note.setWrapText(true);
    note.setPrefRowCount(5);
    validation.getStyleClass().add("error-text");
    validation.setVisible(false);
    validation.managedProperty().bind(validation.visibleProperty());
    title
        .textProperty()
        .addListener((observable, previous, current) -> validation.setVisible(false));
    error.getStyleClass().add("error-text");
    error.setWrapText(true);
    error.setMinHeight(Region.USE_PREF_SIZE);
    error.setVisible(false);
    error.managedProperty().bind(error.visibleProperty());
    save.getStyleClass().add("primary");
    save.setId("save-book");
    save.setDefaultButton(true);
    save.setOnAction(event -> save());
    cancel.setCancelButton(true);
    cancel.setOnAction(event -> requestClose());
    HBox actions = new HBox(10, cancel, save);
    actions.setAlignment(Pos.CENTER_RIGHT);
    VBox form =
        new VBox(
            14,
            heading,
            hint,
            field(messages.text("field.requiredTitle"), title),
            validation,
            field(messages.text("field.author"), author),
            field(messages.text("field.location"), location),
            field(messages.text("field.note"), note));
    ScrollPane scroll = new ScrollPane(form);
    scroll.setFitToWidth(true);
    scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    scroll.setMinHeight(0);
    VBox.setVgrow(scroll, Priority.ALWAYS);
    VBox root = new VBox(14, scroll, error, actions);
    root.setPadding(new Insets(26));
    Scene scene = new Scene(root, 550, 650);
    UiSupport.style(scene);
    scene
        .getAccelerators()
        .put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), this::save);
    // A note must accept Enter even though the dialog has a default save button.
    note.addEventFilter(
        javafx.scene.input.KeyEvent.KEY_PRESSED,
        event -> {
          if (event.getCode() == KeyCode.ENTER && !event.isShortcutDown() && !event.isAltDown()) {
            note.replaceSelection("\n");
            event.consume();
          }
        });
    stage.setScene(scene);
    stage.setOnCloseRequest(
        event -> {
          event.consume();
          requestClose();
        });
  }

  private static VBox field(String text, javafx.scene.control.Control control) {
    Label label = new Label(text);
    label.setLabelFor(control);
    label.getStyleClass().add("field-label");
    VBox box = new VBox(6, label, control);
    if (control instanceof TextArea) {
      VBox.setVgrow(control, Priority.ALWAYS);
    }
    return box;
  }

  void show() {
    stage.show();
    Platform.runLater(title::requestFocus);
  }

  boolean isShowing() {
    return stage.isShowing();
  }

  private Input input() {
    return new Input(title.getText(), author.getText(), location.getText(), note.getText());
  }

  private void save() {
    if (saving) {
      return;
    }
    BookDraft draft;
    try {
      Input current = input();
      draft = new BookDraft(current.title(), current.author(), current.location(), current.note());
    } catch (IllegalArgumentException exception) {
      validation.setText(messages.text("validation.title"));
      validation.setVisible(true);
      title.requestFocus();
      return;
    }
    setSaving(true);
    error.setVisible(false);
    tasks.submit(
        () -> service.save(id, draft),
        book -> {
          setSaving(false);
          onSaved.accept(book);
          stage.hide();
        },
        failure -> {
          setSaving(false);
          error.setText(UiSupport.message(failure, messages));
          error.setVisible(true);
        });
  }

  private void setSaving(boolean value) {
    saving = value;
    save.setDisable(value);
    cancel.setDisable(value);
    title.setDisable(value);
    author.setDisable(value);
    location.setDisable(value);
    note.setDisable(value);
    save.setText(value ? messages.text("save.busy") : messages.text("action.save"));
  }

  void requestClose() {
    requestClose(() -> {});
  }

  void requestClose(Runnable afterClose) {
    if (!saving) {
      closeRequests.request(afterClose);
    }
  }

  private boolean closeIfApproved() {
    if (saving) {
      return false;
    }
    if (!stage.isShowing()
        || input().equals(original)
        || UiSupport.confirm(
            stage,
            messages.text("discard.title"),
            messages.text("discard.detail"),
            messages.text("action.discard"),
            messages.text("action.keep"))) {
      stage.hide();
      return true;
    }
    return false;
  }

  private record Input(String title, String author, String location, String note) {}
}
