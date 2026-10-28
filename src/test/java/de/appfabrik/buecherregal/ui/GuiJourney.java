package de.appfabrik.buecherregal.ui;

import de.appfabrik.buecherregal.domain.BookDraft;
import de.appfabrik.buecherregal.i18n.AppLanguage;
import de.appfabrik.buecherregal.i18n.Messages;
import de.appfabrik.buecherregal.persistence.LanguagePreferences;
import de.appfabrik.buecherregal.persistence.SqliteBookRepository;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.PixelFormat;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;

/** Drives the production application with actual mouse/keyboard events on an isolated display. */
public final class GuiJourney {
  private static final List<String> checks = new ArrayList<>();
  private static String failure;

  private GuiJourney() {}

  public static void main(String[] args) throws Exception {
    if (args.length != 4) {
      throw new IllegalArgumentException("data-dir output-dir locale mode");
    }
    Path data = Path.of(args[0]).toAbsolutePath();
    Path output = Path.of(args[1]).toAbsolutePath();
    String tag = args[2];
    String mode = args[3];
    if (!Files.isRegularFile(data.resolve(".synthetic-bookshelf-demo"))) {
      throw new IllegalArgumentException(
          "An isolated, explicitly marked synthetic data directory is required");
    }
    Files.createDirectories(output);
    AppLanguage language = AppLanguage.resolve(tag, Locale.US);
    Messages messages = new Messages(language);
    Properties demo = new Properties();
    try (var reader =
        Files.newBufferedReader(
            Path.of("assets/store_demo", tag + ".properties"), StandardCharsets.UTF_8)) {
      demo.load(reader);
    }
    if (!mode.equals("restart")) {
      if (Files.exists(data.resolve("buecherregal.db"))) {
        throw new IllegalArgumentException("Fresh demo directory required");
      }
      var repository = new SqliteBookRepository(data.resolve("buecherregal.db"));
      repository.initialize();
      for (int i = 1; i <= 5; i++) {
        repository.create(
            new BookDraft(
                demo.getProperty("book." + i + ".title"),
                demo.getProperty("book." + i + ".author"),
                demo.getProperty("book." + i + ".location"),
                demo.getProperty("book." + i + ".note")));
      }
      new LanguagePreferences(data).save(tag);
    }
    // A deliberately different system language proves the saved preference wins at real startup.
    Locale.setDefault(Locale.JAPAN);
    Thread driver =
        new Thread(
            () -> {
              try {
                waitFor(
                    () -> find("book-count") instanceof Label label && !label.getText().isEmpty());
                check(
                    fx(() -> ((Button) find("add-book")).getText())
                        .equals("+  " + messages.text("book.add")),
                    "saved language applied at startup");
                if (mode.equals("capture")) {
                  captureJourney(output, messages, demo);
                } else if (mode.equals("verify")) {
                  verifyJourney(data, output, messages);
                } else if (mode.equals("restart")) {
                  check(
                      new LanguagePreferences(data).load().equals(tag),
                      "preference survived process restart");
                  var books = new SqliteBookRepository(data.resolve("buecherregal.db")).findAll();
                  check(
                      books.stream()
                          .anyMatch(
                              book ->
                                  book.title().equals("L'été d'Álvaro – ação – Bücher")
                                      && book.note().contains("Line 30")),
                      "Unicode and multiline draft survived restart");
                  screenshot(output.resolve("restart.png"));
                } else {
                  throw new IllegalArgumentException("Unknown mode");
                }
                Files.write(output.resolve("checks.txt"), checks, StandardCharsets.UTF_8);
              } catch (Exception | AssertionError exception) {
                failure = exception.toString();
                exception.printStackTrace();
                try {
                  screenshot(output.resolve("failure.png"));
                } catch (Exception ignored) {
                  /* Toolkit may be unavailable. */
                }
              } finally {
                Platform.runLater(
                    () -> {
                      for (Window window : List.copyOf(Window.getWindows())) {
                        window.hide();
                      }
                      Platform.exit();
                    });
              }
            },
            "bookshelf-gui-driver");
    driver.setDaemon(true);
    driver.start();
    Application.launch(BooksApplication.class, "--data-dir=" + data);
    driver.join(10000);
    if (failure != null) {
      throw new IllegalStateException(failure);
    }
    if (driver.isAlive()) {
      throw new IllegalStateException("GUI driver did not finish");
    }
    System.out.println("GUI " + mode + " passed: " + tag + " (" + checks.size() + " checks)");
  }

  private static void captureJourney(Path output, Messages messages, Properties demo)
      throws Exception {
    fx(
        () -> {
          Stage main = (Stage) find("add-book").getScene().getWindow();
          main.setX(0);
          main.setY(0);
          main.setWidth(1920);
          main.setHeight(1080);
          return null;
        });
    pause();
    clickTableFirstRow();
    screenshot(output.resolve("01-overview.png"));
    click("edit-book");
    waitFor(() -> find("book-title") != null);
    screenshot(output.resolve("02-edit.png"));
    click("cancel-book");
    waitFor(() -> find("book-title") == null);
    enter("search-books", demo.getProperty("search"));
    waitFor(() -> ((TableView<?>) find("books-table")).getItems().size() == 1);
    screenshot(output.resolve("03-search.png"));
    click("clear-search");
    click("language-settings");
    waitFor(() -> find("language-choice") != null);
    click("language-choice");
    screenshot(output.resolve("04-language.png"));
    key(KeyCode.ESCAPE);
    click("language-cancel");
    check(true, "four real 1920x1080 screen captures");
    check(
        fx(() -> ((Label) find("book-count")).getText()).equals(messages.count(5, 5, false)),
        "localized collection count");
  }

  private static void verifyJourney(Path data, Path output, Messages messages) throws Exception {
    fx(
        () -> {
          Stage main = (Stage) find("add-book").getScene().getWindow();
          main.setWidth(760);
          main.setHeight(620);
          return null;
        });
    pause();
    checkButtonsFit();
    screenshot(output.resolve("minimum-main.png"));
    click("add-book");
    waitFor(() -> find("save-book") != null);
    click("save-book");
    waitFor(() -> find("title-validation") instanceof Label label && label.isVisible());
    check(
        fx(() -> ((Label) find("title-validation")).getText())
            .equals(messages.text("validation.title")),
        "localized required-title validation");
    String title = "L'été d'Álvaro – ação – Bücher";
    enter("book-title", title);
    enter("book-author", "Élise O'Connor");
    enter("book-location", "São Paulo · étagère 12");
    enter(
        "book-note",
        "Line 1 – Grüße, coração, français\\n".replace("\\n", "\n")
            + "A long note with accents.\n".repeat(28)
            + "Line 30");
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.keep")) != null);
    check(
        fx(() -> confirmationButton(messages.text("action.keep")).isDefaultButton()),
        "keep editing is the safe default");
    key(KeyCode.ENTER);
    waitFor(() -> confirmationButton(messages.text("action.keep")) == null);
    check(
        fx(() -> ((TextInputControl) find("book-title")).getText()).equals(title),
        "draft survives Escape and continue");
    // Force a real SQLite write failure while preserving the entire edit form.
    try (var connection =
            DriverManager.getConnection("jdbc:sqlite:" + data.resolve("buecherregal.db"));
        var statement = connection.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      click("save-book");
      waitFor(() -> find("save-error") instanceof Label label && label.isVisible());
      check(
          fx(() -> ((Label) find("save-error")).getText())
              .equals(messages.text("storage.OPERATION")),
          "localized storage failure");
      check(
          fx(() -> ((TextInputControl) find("book-note")).getText()).contains("Line 30"),
          "full draft survives database lock");
      fx(
          () -> {
            Stage editor = (Stage) find("book-title").getScene().getWindow();
            editor.setWidth(480);
            editor.setHeight(590);
            return null;
          });
      pause();
      checkButtonsFit();
      screenshot(output.resolve("minimum-editor-error.png"));
      statement.execute("ROLLBACK");
    }
    click("save-book");
    waitFor(() -> find("save-book") == null);
    check(
        new SqliteBookRepository(data.resolve("buecherregal.db")).findAll().size() == 6,
        "retry saves exactly one book");
    enter("search-books", "ÁLVARO");
    waitFor(() -> ((TableView<?>) find("books-table")).getItems().size() == 1);
    clickTableFirstRow();
    click("delete-book");
    waitFor(() -> confirmationButton(messages.text("action.cancel")) != null);
    check(
        fx(() -> confirmationButton(messages.text("action.cancel")).isDefaultButton()),
        "delete defaults to cancel");
    key(KeyCode.ESCAPE);
    waitFor(() -> confirmationButton(messages.text("action.cancel")) == null);
    check(
        new SqliteBookRepository(data.resolve("buecherregal.db")).findAll().size() == 6,
        "cancelled deletion preserves data");
    click("clear-search");
    click("language-settings");
    waitFor(() -> find("language-choice") != null);
    Path preference = data.resolve("language.txt");
    Path backup = data.resolve("language.before-test");
    Files.move(preference, backup);
    Files.createDirectory(preference);
    Files.writeString(preference.resolve("blocker"), "synthetic");
    click("language-save");
    waitFor(() -> find("language-error") instanceof Label label && label.isVisible());
    check(
        fx(() -> ((Label) find("language-error")).getText())
            .equals(messages.text("language.saveError")),
        "settings write failure preserves dialog");
    Files.delete(preference.resolve("blocker"));
    Files.delete(preference);
    Files.move(backup, preference);
    click("language-choice");
    key(KeyCode.HOME);
    // automatic, German, English. Switch every language to another language for the restart test.
    String next = messages.locale().getLanguage().equals("en") ? "de-DE" : "en-US";
    key(KeyCode.DOWN);
    if (next.equals("en-US")) {
      key(KeyCode.DOWN);
    }
    key(KeyCode.ENTER);
    click("language-save");
    waitFor(() -> find("language-choice") == null);
    check(new LanguagePreferences(data).load().equals(next), "new language selection saved");
    check(
        fx(() -> ((Label) find("status-message")).getText())
            .equals(messages.text("language.saved")),
        "restart notice in current language");
    Files.writeString(output.resolve("restart-locale.txt"), next + "\n");
  }

  private static void checkButtonsFit() throws Exception {
    fx(
        () -> {
          for (Window window : Window.getWindows()) {
            if (!window.isShowing() || window.getScene() == null) {
              continue;
            }
            for (Node node : window.getScene().getRoot().lookupAll(".button")) {
              if (node instanceof Button button && button.isVisible()) {
                check(
                    button.getWidth() + 1 >= button.prefWidth(-1),
                    "button fits: " + button.getText());
                Bounds bounds = button.localToScene(button.getBoundsInLocal());
                check(
                    bounds.getMaxY() <= window.getScene().getHeight() + 1,
                    "button is within window");
              }
            }
          }
          return null;
        });
  }

  private static void clickTableFirstRow() throws Exception {
    fx(
        () -> {
          Node row = find("books-table").lookup(".table-row-cell");
          if (row == null) {
            throw new AssertionError("No visible table row");
          }
          mouse(row);
          return null;
        });
    pause();
  }

  private static Button confirmationButton(String text) {
    for (Window window : Window.getWindows()) {
      if (window.isShowing() && window.getScene() != null) {
        for (Node node : window.getScene().getRoot().lookupAll(".button")) {
          if (node instanceof Button button
              && button.getText().equals(text)
              && button.getScene().getRoot().getStyleClass().contains("dialog-pane")) {
            return button;
          }
        }
      }
    }
    return null;
  }

  private static Node find(String id) {
    for (Window window : Window.getWindows()) {
      if (window.isShowing() && window.getScene() != null) {
        Node node = window.getScene().lookup("#" + id);
        if (node != null && node.isVisible()) {
          return node;
        }
      }
    }
    return null;
  }

  private static void click(String id) throws Exception {
    fx(
        () -> {
          Node node = find(id);
          if (node == null || node.isDisabled()) {
            throw new AssertionError("Control unavailable: " + id);
          }
          mouse(node);
          return null;
        });
    pause();
  }

  private static void mouse(Node node) {
    Bounds b = node.localToScreen(node.getBoundsInLocal());
    Robot robot = new Robot();
    robot.mouseMove(b.getCenterX(), b.getCenterY());
    robot.mouseClick(MouseButton.PRIMARY);
  }

  private static void enter(String id, String text) throws Exception {
    click(id);
    fx(
        () -> {
          ClipboardContent content = new ClipboardContent();
          content.putString(text);
          Clipboard.getSystemClipboard().setContent(content);
          Robot robot = new Robot();
          robot.keyPress(KeyCode.CONTROL);
          robot.keyType(KeyCode.A);
          robot.keyType(KeyCode.V);
          robot.keyRelease(KeyCode.CONTROL);
          return null;
        });
    pause();
    check(fx(() -> ((TextInputControl) find(id)).getText()).equals(text), "keyboard input: " + id);
  }

  private static void key(KeyCode key) throws Exception {
    fx(
        () -> {
          new Robot().keyType(key);
          return null;
        });
    pause();
  }

  private static void screenshot(Path file) throws Exception {
    pause();
    BufferedImage image =
        fx(
            () -> {
              var capture = new Robot().getScreenCapture(null, 0, 0, 1920, 1080, false);
              int[] pixels = new int[1920 * 1080];
              capture
                  .getPixelReader()
                  .getPixels(0, 0, 1920, 1080, PixelFormat.getIntArgbInstance(), pixels, 0, 1920);
              BufferedImage result = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
              result.setRGB(0, 0, 1920, 1080, pixels, 0, 1920);
              return result;
            });
    if (!ImageIO.write(image, "png", file.toFile())) {
      throw new IllegalStateException("PNG encoder missing");
    }
  }

  private static <T> T fx(Callable<T> operation) throws Exception {
    FutureTask<T> task = new FutureTask<>(operation);
    Platform.runLater(task);
    return task.get(10, TimeUnit.SECONDS);
  }

  private static void waitFor(Callable<Boolean> condition) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
    while (System.nanoTime() < deadline) {
      try {
        if (fx(condition)) {
          return;
        }
      } catch (IllegalStateException notStarted) {
        /* JavaFX startup is asynchronous. */
      }
      Thread.sleep(100);
    }
    throw new AssertionError("Timed out waiting for GUI state");
  }

  private static void pause() throws InterruptedException {
    Thread.sleep(250);
  }

  private static void check(boolean passed, String description) {
    if (!passed) {
      throw new AssertionError(description);
    }
    checks.add(description);
  }
}
