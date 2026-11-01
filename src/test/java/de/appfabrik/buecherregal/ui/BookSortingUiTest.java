package de.appfabrik.buecherregal.ui;

import static de.appfabrik.buecherregal.ui.FxTestSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import de.appfabrik.buecherregal.application.BookService;
import de.appfabrik.buecherregal.application.CollectionTransfer;
import de.appfabrik.buecherregal.domain.Book;
import de.appfabrik.buecherregal.domain.BookDraft;
import de.appfabrik.buecherregal.i18n.AppLanguage;
import de.appfabrik.buecherregal.i18n.Messages;
import de.appfabrik.buecherregal.persistence.JsonCollectionFile;
import de.appfabrik.buecherregal.persistence.LanguagePreferences;
import de.appfabrik.buecherregal.persistence.SqliteBookRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.TableColumnHeader;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.Robot;
import javafx.stage.Stage;
import javafx.stage.Window;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@EnabledIfSystemProperty(named = "buecherregal.uiTests", matches = "true")
class BookSortingUiTest {
  @TempDir Path directory;
  private Stage stage;
  private AsyncTasks tasks;
  private TableView<?> table;
  private BookService service;
  private Book alpha;
  private Book zulu;

  @BeforeAll
  static void toolkit() throws Exception {
    FxTestSupport.start();
  }

  private void open(AppLanguage language) throws Exception {
    var repository = new SqliteBookRepository(directory.resolve("synthetic.db"));
    service = new BookService(repository);
    service.load();
    zulu = service.save(null, new BookDraft("Zulu", "Anna", "Z shelf", "Zulu note"));
    alpha = service.save(null, new BookDraft("alpha", "Zoë", "A shelf", "Alpha note"));
    tasks = new AsyncTasks();
    stage =
        fx(
            () -> {
              Stage owner = new Stage();
              new MainWindow(
                      owner,
                      service,
                      new CollectionTransfer(repository, new JsonCollectionFile(directory)),
                      tasks,
                      new Messages(language),
                      new LanguagePreferences(directory),
                      language.tag(),
                      false)
                  .show();
              return owner;
            });
    await(() -> uncheckedFx(() -> !((Button) stage.getScene().lookup("#add-book")).isDisabled()));
    table = fx(() -> (TableView<?>) stage.getScene().lookup("#books-table"));
    fx(
        () -> {
          stage.toFront();
          stage.requestFocus();
          return null;
        });
    assertEquals(alpha.id(), fx(() -> ((Book) table.getItems().getFirst()).id()));
  }

  @AfterEach
  void close() throws Exception {
    fx(
        () -> {
          for (Window window : List.copyOf(Window.getWindows())) window.hide();
          return null;
        });
    if (tasks != null) tasks.close();
  }

  @ParameterizedTest
  @EnumSource(AppLanguage.class)
  void nativeHeaderClicksToggleEveryColumnAndPreserveSelectionInEveryLanguage(AppLanguage language)
      throws Exception {
    open(language);
    byte[] database = Files.readAllBytes(directory.resolve("synthetic.db"));
    var columns = fx(() -> List.copyOf(table.getColumns()));
    fx(
        () -> {
          table.getSelectionModel().select(table.getItems().indexOf(alpha));
          return null;
        });
    long[] ascendingFirst = {alpha.id(), zulu.id(), alpha.id()};
    for (int index = 0; index < columns.size(); index++) {
      TableColumn<?, ?> column = columns.get(index);
      for (int click = 0; click < 4; click++) {
        boolean descending =
            fx(
                () ->
                    table.getSortOrder().contains(column)
                        && column.getSortType() == TableColumn.SortType.ASCENDING);
        long expected =
            descending
                ? (ascendingFirst[index] == alpha.id() ? zulu.id() : alpha.id())
                : ascendingFirst[index];
        clickHeader(column);
        await(() -> uncheckedFx(() -> ((Book) table.getItems().getFirst()).id() == expected));
        fx(
            () -> {
              assertEquals(List.of(column), table.getSortOrder());
              assertFalse(column.isReorderable());
              assertEquals(columns, table.getColumns());
              var arrows = column.getGraphic().lookupAll(".sort-chevron");
              assertEquals(2, arrows.size());
              assertEquals(1, arrows.stream().filter(arrow -> arrow.getOpacity() == 1).count());
              assertEquals(alpha.id(), ((Book) table.getSelectionModel().getSelectedItem()).id());
              assertEquals(
                  "Alpha note", ((TextArea) stage.getScene().lookup("#selected-note")).getText());
              return null;
            });
      }
    }
    fx(
        () -> {
          var search = (TextField) stage.getScene().lookup("#search-books");
          search.setText("alpha");
          assertEquals(1, table.getItems().size());
          search.clear();
          assertEquals(zulu.id(), ((Book) table.getItems().getFirst()).id());
          assertEquals(alpha.id(), ((Book) table.getSelectionModel().getSelectedItem()).id());
          assertEquals(List.of(columns.getLast()), table.getSortOrder());
          assertEquals(TableColumn.SortType.DESCENDING, columns.getLast().getSortType());
          return null;
        });
    assertArrayEquals(database, Files.readAllBytes(directory.resolve("synthetic.db")));
    if (language == AppLanguage.GERMAN) capture();
  }

  @Test
  void editingResortsByActiveColumnAndKeepsTheSavedBookSelected() throws Exception {
    open(AppLanguage.ENGLISH);
    var author = fx(() -> table.getColumns().get(1));
    clickHeader(author);
    await(() -> uncheckedFx(() -> ((Book) table.getItems().getFirst()).id() == zulu.id()));
    fx(
        () -> {
          table.getSelectionModel().select(table.getItems().indexOf(alpha));
          ((Button) stage.getScene().lookup("#edit-book")).fire();
          return null;
        });
    await(() -> uncheckedFx(() -> Window.getWindows().stream().anyMatch(w -> w != stage)));
    Stage editor =
        fx(
            () ->
                Window.getWindows().stream()
                    .filter(w -> w != stage)
                    .map(Stage.class::cast)
                    .findFirst()
                    .orElseThrow());
    await(() -> uncheckedFx(() -> editor.getOpacity() == 1));
    fx(
        () -> {
          assertEquals("alpha", ((TextField) editor.getScene().lookup("#book-title")).getText());
          ((TextField) editor.getScene().lookup("#book-author")).setText("Aaron");
          ((Button) editor.getScene().lookup("#save-book")).fire();
          return null;
        });
    await(() -> uncheckedFx(() -> !editor.isShowing()));
    fx(
        () -> {
          assertEquals(List.of(author), table.getSortOrder());
          assertEquals(TableColumn.SortType.ASCENDING, author.getSortType());
          assertEquals(alpha.id(), ((Book) table.getItems().getFirst()).id());
          assertEquals(alpha.id(), ((Book) table.getSelectionModel().getSelectedItem()).id());
          assertEquals(
              "Alpha note", ((TextArea) stage.getScene().lookup("#selected-note")).getText());
          return null;
        });
    assertEquals(
        "Aaron",
        service.load().stream()
            .filter(b -> b.id() == alpha.id())
            .findFirst()
            .orElseThrow()
            .author());
  }

  private void clickHeader(TableColumn<?, ?> column) throws Exception {
    fx(
        () -> {
          TableColumnHeader header =
              table.lookupAll(".column-header").stream()
                  .filter(TableColumnHeader.class::isInstance)
                  .map(TableColumnHeader.class::cast)
                  .filter(h -> h.getTableColumn() == column)
                  .findFirst()
                  .orElseThrow();
          var bounds = header.localToScreen(header.getBoundsInLocal());
          var robot = new Robot();
          robot.mouseMove(bounds.getCenterX(), bounds.getCenterY());
          robot.mouseClick(MouseButton.PRIMARY);
          return null;
        });
  }

  private void capture() throws Exception {
    var image = fx(() -> stage.getScene().snapshot(null));
    int width = (int) image.getWidth(), height = (int) image.getHeight();
    var buffered =
        new java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < height; y++)
      for (int x = 0; x < width; x++) buffered.setRGB(x, y, image.getPixelReader().getArgb(x, y));
    Path output = Path.of("target/sorting-preview.png");
    ImageIO.write(buffered, "png", output.toFile());
    buffered.flush();
  }
}
