package de.appfabrik.buecherregal.ui;

import de.appfabrik.buecherregal.domain.BookDraft;
import de.appfabrik.buecherregal.i18n.AppLanguage;
import de.appfabrik.buecherregal.i18n.Messages;
import de.appfabrik.buecherregal.i18n.PrivacyNotice;
import de.appfabrik.buecherregal.persistence.JsonCollectionFile;
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
import javafx.scene.control.MenuButton;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
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
  private static final int WIDTH = Integer.getInteger("bookshelf.captureWidth", 1920);
  private static final int HEIGHT = Integer.getInteger("bookshelf.captureHeight", 1080);
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
    AppLanguage language =
        mode.equals("startup") ? AppLanguage.ENGLISH : AppLanguage.resolve(tag, Locale.US);
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
      if (!mode.equals("startup")) {
        new LanguagePreferences(data).save(tag);
      }
    }
    // A deliberately different system language proves the saved preference wins at real startup.
    Locale.setDefault(mode.equals("startup") ? Locale.GERMANY : Locale.JAPAN);
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
                if (mode.equals("startup")) {
                  check(
                      !Files.exists(data.resolve("language.txt")),
                      "first start does not write a language choice");
                  check(
                      new LanguagePreferences(data).load().equals("en-US"),
                      "first start uses English on a German system");
                  screenshot(output.resolve("startup.png"));
                } else if (mode.equals("capture")) {
                  captureJourney(output, messages, demo);
                } else if (mode.equals("store")) {
                  storeJourney(data, output, messages, demo);
                } else if (mode.equals("transfer")) {
                  transferJourney(data, output, messages);
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
          main.setWidth(WIDTH);
          main.setHeight(HEIGHT);
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
    check(true, "four real screen captures at " + WIDTH + "x" + HEIGHT);
    check(
        fx(() -> ((Label) find("book-count")).getText()).equals(messages.count(5, 5, false)),
        "localized collection count");
  }

  private static void storeJourney(Path data, Path output, Messages messages, Properties demo)
      throws Exception {
    captureJourney(output, messages, demo);
    Files.delete(output.resolve("04-language.png"));
    Platform.runLater(
        () ->
            SandboxExportDialog.filename(
                find("add-book").getScene().getWindow(),
                messages,
                data,
                "bookshelf-sammlung.json"));
    waitFor(() -> find("export-filename") != null);
    enter("export-filename", "../outside");
    check(fx(() -> find("export-filename-confirm").isDisabled()), "folder grant cannot be escaped");
    enter("export-filename", "bookshelf-sammlung.json");
    check(!fx(() -> find("export-filename-confirm").isDisabled()), "valid export name enabled");
    screenshot(output.resolve("04-json-export.png"));
    key(KeyCode.ESCAPE);
    waitFor(() -> find("export-filename") == null);
    checkButtonsFit();
  }

  private static void transferJourney(Path data, Path output, Messages messages) throws Exception {
    check(fx(() -> find("reload-books") == null), "reload button removed");
    check(
        fx(() -> find("export-books") != null && find("import-books") != null),
        "both collection actions visible");
    Path database = data.resolve("buecherregal.db");
    byte[] initialDatabase = Files.readAllBytes(database);
    var repository = new SqliteBookRepository(database);
    var original = repository.findAll();
    var files = new JsonCollectionFile(data);
    click("export-books");
    nativeFile(messages.text("transfer.exportTitle"), null, true);
    waitStatus(messages.text("transfer.cancelled"));
    click("import-books");
    nativeFile(messages.text("transfer.importTitle"), null, false);
    waitStatus(messages.text("transfer.cancelled"));
    check(
        java.util.Arrays.equals(initialDatabase, Files.readAllBytes(database)),
        "native cancellations preserve database");

    enter("search-books", "no matching book for transfer check");
    waitFor(() -> ((TableView<?>) find("books-table")).getItems().isEmpty());
    Path exported = output.resolve("Sammlung für Ömer.json");
    click("export-books");
    nativeFile(messages.text("transfer.exportTitle"), exported, true);
    waitStatus(messages.text("transfer.exported", 5));
    check(
        files.read(exported).equals(original.stream().map(book -> book.draft()).toList()),
        "native export contains all five books despite empty search result");
    check(
        java.util.Arrays.equals(initialDatabase, Files.readAllBytes(database)),
        "export preserves database bytes");

    Path broken = output.resolve("broken.json");
    Files.writeString(broken, "{}");
    click("import-books");
    nativeFile(messages.text("transfer.importTitle"), broken, false);
    waitStatus(messages.text("transfer.error.FORMAT"));
    check(
        java.util.Arrays.equals(initialDatabase, Files.readAllBytes(database)),
        "invalid import makes no changes");

    Path incoming = output.resolve("incoming.json");
    var drafts = new ArrayList<>(original.stream().map(book -> book.draft()).toList());
    drafts.add(original.getFirst().draft());
    drafts.add(new BookDraft("Import café – 日本語 📚", "Élise", "Étagère 2", "First\nSecond line"));
    files.write(drafts, files.inspect(incoming), false);
    click("import-books");
    nativeFile(messages.text("transfer.importTitle"), incoming, false);
    waitFor(() -> confirmationButton(messages.text("action.import")) != null);
    key(KeyCode.ENTER);
    waitStatus(messages.text("transfer.cancelled"));
    check(
        java.util.Arrays.equals(initialDatabase, Files.readAllBytes(database)),
        "import confirmation defaults to cancellation");

    importAndConfirm(incoming, messages);
    waitStatus(messages.text("transfer.imported", 2, 5));
    check(
        fx(() -> ((TextInputControl) find("search-books")).getText()).isEmpty(),
        "successful import clears search");
    var restored = repository.findAll();
    check(
        restored.size() == 7 && restored.containsAll(original),
        "import preserves existing identities and adds missing copies");
    check(
        restored.getLast().draft().equals(drafts.getLast()),
        "Unicode and multiline note imported exactly");
    importAndConfirm(incoming, messages);
    waitStatus(messages.text("transfer.imported", 0, 7));
    check(repository.findAll().equals(restored), "repeated import does not duplicate books");

    try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        var statement = connection.createStatement()) {
      statement.execute("BEGIN EXCLUSIVE");
      importAndConfirm(incoming, messages);
      waitStatus(messages.text("transfer.error.IMPORT"));
      statement.execute("ROLLBACK");
    }
    check(repository.findAll().equals(restored), "failed import preserves collection");
    importAndConfirm(incoming, messages);
    waitStatus(messages.text("transfer.imported", 0, 7));
    check(repository.findAll().equals(restored), "import can be retried after storage failure");
    check(
        fx(() -> !find("export-books").isDisabled() && !find("import-books").isDisabled()),
        "buttons enabled after failure and retry");
    fx(
        () -> {
          Stage main = (Stage) find("add-book").getScene().getWindow();
          main.setWidth(760);
          main.setHeight(620);
          return null;
        });
    pause();
    checkButtonsFit();
    screenshot(output.resolve("transfer-complete.png"));
  }

  private static void importAndConfirm(Path path, Messages messages) throws Exception {
    click("import-books");
    nativeFile(messages.text("transfer.importTitle"), path, false);
    waitFor(() -> confirmationButton(messages.text("action.import")) != null);
    fx(
        () -> {
          mouse(confirmationButton(messages.text("action.import")));
          return null;
        });
    pause();
  }

  private static void waitStatus(String expected) throws Exception {
    waitFor(() -> ((Label) find("status-message")).getText().equals(expected));
  }

  private static void nativeFile(String title, Path path, boolean save) throws Exception {
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    String window = null;
    while (System.nanoTime() < deadline) {
      Process search =
          new ProcessBuilder("xdotool", "search", "--onlyvisible", "--name", "^" + title + "$")
              .start();
      String found =
          new String(search.getInputStream().readAllBytes(), StandardCharsets.UTF_8).strip();
      if (search.waitFor() == 0 && !found.isBlank()) {
        window = found.lines().findFirst().orElseThrow();
        break;
      }
      Thread.sleep(80);
    }
    if (window == null) {
      throw new AssertionError("Native file chooser missing: " + title);
    }
    command("xdotool", "windowfocus", window);
    if (path == null) {
      command("xdotool", "key", "Escape");
      return;
    }
    String filename = path.toAbsolutePath().toString();
    if (save) {
      filename = filename.substring(0, filename.length() - ".json".length());
    }
    Process clipboard = new ProcessBuilder("xsel", "--clipboard", "--input").start();
    try (var input = clipboard.getOutputStream()) {
      input.write(filename.getBytes(StandardCharsets.UTF_8));
    }
    if (clipboard.waitFor() != 0) {
      throw new AssertionError("Native clipboard failed");
    }
    command("xdotool", "key", "--clearmodifiers", "ctrl+l");
    Thread.sleep(120);
    command("xdotool", "key", "--clearmodifiers", "ctrl+v");
    Thread.sleep(120);
    command("xdotool", "key", "Return");
    Thread.sleep(300);
  }

  private static void command(String... arguments) throws Exception {
    Process process = new ProcessBuilder(arguments).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    if (process.waitFor() != 0) {
      throw new AssertionError(output);
    }
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
    privacyJourney(data, output, messages);
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

  private static void privacyJourney(Path data, Path output, Messages messages) throws Exception {
    byte[] database = Files.readAllBytes(data.resolve("buecherregal.db"));
    byte[] preference = Files.readAllBytes(data.resolve("language.txt"));
    click("app-info");
    key(KeyCode.DOWN);
    key(KeyCode.ENTER);
    waitFor(() -> find("privacy-text") != null);
    String expected = PrivacyNotice.text(messages);
    check(
        fx(() -> ((TextArea) find("privacy-text")).getText()).equals(expected),
        "complete offline privacy text in selected language");
    check(!fx(() -> ((TextArea) find("privacy-text")).isEditable()), "privacy text is read-only");
    fx(
        () -> {
          Stage dialog = (Stage) find("privacy-text").getScene().getWindow();
          dialog.setWidth(440);
          dialog.setHeight(380);
          return null;
        });
    pause();
    checkButtonsFit();
    click("privacy-text");
    fx(
        () -> {
          Robot robot = new Robot();
          robot.keyPress(KeyCode.CONTROL);
          robot.keyType(KeyCode.A);
          robot.keyType(KeyCode.C);
          robot.keyRelease(KeyCode.CONTROL);
          return null;
        });
    pause();
    check(
        fx(() -> Clipboard.getSystemClipboard().getString()).equals(expected),
        "entire privacy policy copied with keyboard");
    fx(
        () -> {
          Robot robot = new Robot();
          robot.keyPress(KeyCode.CONTROL);
          robot.keyType(KeyCode.END);
          robot.keyRelease(KeyCode.CONTROL);
          return null;
        });
    pause();
    check(fx(() -> ((TextArea) find("privacy-text")).getScrollTop()) > 0, "long policy scrolls");
    screenshot(output.resolve("privacy-small.png"));
    key(KeyCode.ESCAPE);
    waitFor(() -> find("privacy-text") == null);
    click("app-info");
    key(KeyCode.DOWN);
    key(KeyCode.ENTER);
    waitFor(() -> find("privacy-close") != null);
    screenshot(output.resolve("privacy.png"));
    click("privacy-close");
    waitFor(() -> find("privacy-text") == null);
    check(
        java.util.Arrays.equals(database, Files.readAllBytes(data.resolve("buecherregal.db"))),
        "privacy dialog preserves database byte-for-byte");
    check(
        java.util.Arrays.equals(preference, Files.readAllBytes(data.resolve("language.txt"))),
        "privacy dialog preserves language preference byte-for-byte");
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
                    bounds.getMaxY() <= window.getScene().getHeight() + 1
                        && bounds.getMaxX() <= window.getScene().getWidth() + 1,
                    "button is within window: " + button.getText());
              }
            }
            for (Node node : window.getScene().getRoot().lookupAll(".menu-button")) {
              if (node instanceof MenuButton button && button.isVisible()) {
                Bounds bounds = button.localToScene(button.getBoundsInLocal());
                check(button.getWidth() + 1 >= button.prefWidth(-1), "info menu label fits");
                check(
                    bounds.getMaxX() <= window.getScene().getWidth() + 1
                        && bounds.getMaxY() <= window.getScene().getHeight() + 1,
                    "info menu is within window");
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
              var capture = new Robot().getScreenCapture(null, 0, 0, WIDTH, HEIGHT, false);
              int[] pixels = new int[WIDTH * HEIGHT];
              capture
                  .getPixelReader()
                  .getPixels(
                      0, 0, WIDTH, HEIGHT, PixelFormat.getIntArgbInstance(), pixels, 0, WIDTH);
              BufferedImage result = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
              result.setRGB(0, 0, WIDTH, HEIGHT, pixels, 0, WIDTH);
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
