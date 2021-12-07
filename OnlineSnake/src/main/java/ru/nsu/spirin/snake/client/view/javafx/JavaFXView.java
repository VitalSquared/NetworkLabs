package ru.nsu.spirin.snake.client.view.javafx;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import me.ippolitov.fit.snakes.SnakesProto;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import org.jetbrains.annotations.NotNull;
import ru.nsu.spirin.snake.gamehandler.GameState;
import ru.nsu.spirin.snake.gamehandler.Player;
import ru.nsu.spirin.snake.gamehandler.Snake;
import ru.nsu.spirin.snake.client.view.GameView;
import ru.nsu.spirin.snake.client.controller.GameController;
import ru.nsu.spirin.snake.client.controller.events.ExitEvent;
import ru.nsu.spirin.snake.client.controller.events.JoinToGameEvent;
import ru.nsu.spirin.snake.client.controller.events.MoveEvent;
import ru.nsu.spirin.snake.client.controller.events.NewGameEvent;
import ru.nsu.spirin.snake.multicastreceiver.GameInfo;
import ru.nsu.spirin.snake.utils.PlayerUtils;
import ru.nsu.spirin.snake.utils.StateUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public final class JavaFXView implements GameView {
    private static final Paint FRUIT_COLOR = Color.GREEN;
    private static final Paint EMPTY_CELL_COLOR = Color.WHITE;

    private @FXML TableColumn<ActiveGameButton, String> masterColumn;
    private @FXML TableColumn<ActiveGameButton, Integer> playersNumberColumn;
    private @FXML TableColumn<ActiveGameButton, String> fieldSizeColumn;
    private @FXML TableColumn<ActiveGameButton, String> foodColumn;
    private @FXML TableColumn<ActiveGameButton, Button> connectButtonColumn;
    private @FXML TableColumn<Player, String> playerNameColumn;
    private @FXML TableColumn<Player, Integer> playerScoreColumn;
    private @FXML Label gameOwner;
    private @FXML Label foodAmount;
    private @FXML Label fieldSize;
    private @FXML TableView<Player> playersRankingTable;
    private @FXML Button exitButton;
    private @FXML Button newGameButton;
    private @FXML TableView<ActiveGameButton> gameListTable;
    private @FXML BorderPane gameFieldPane;

    private final ObservableList<Player> playersObservableList = FXCollections.observableArrayList();
    private final ObservableList<ActiveGameButton> gameInfoObservableList = FXCollections.observableArrayList();
    private final Set<ActiveGameButton> activeGameButtons = new HashSet<>();
    private final PlayerColorMapper colorMapper = new PlayerColorMapper();

    private Rectangle[][] fieldCells;
    private Stage stage;
    private GameConfig gameConfig;
    private GameController gameController;

    public void setGameController(@NotNull GameController controller) {
        this.gameController = controller;
    }

    public void setStage(@NotNull Stage stage) {
        this.stage = stage;
        this.stage.setOnCloseRequest(event -> close(true));
        this.stage.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (null == this.gameController) {
                throw new IllegalStateException("Cant move with undefined controller");
            }
            getDirectionByKeyCode(event.getCode()).ifPresent(direction -> this.gameController.fireEvent(new MoveEvent(direction)));
        });
        initPlayersInfoTable();
        initGameListTable();
        setActionOnButtons();
    }

    @Override
    public void updateCurrentGame(GameState state) {
        Platform.runLater(() -> {
            this.foodAmount.setText(String.valueOf(state.getFruits().size()));
            this.fieldSize.setText(state.getGameConfig().getHeight() + "x" + state.getGameConfig().getWidth());
            this.gameOwner.setText(StateUtils.getMasterNameFromState(state));
        });
        this.playersObservableList.setAll(state.getActivePlayers());
        updateField(state);
    }

    @Override
    public void setConfig(@NotNull GameConfig gameConfig) {
        this.gameConfig = gameConfig;
        buildField();
    }

    @Override
    public void updateGameList(@NotNull Collection<GameInfo> gameInfos) {
        this.activeGameButtons.clear();
        gameInfos.forEach(gameInfo -> {
            ActiveGameButton activeGameButton = new ActiveGameButton(gameInfo);
            this.activeGameButtons.add(activeGameButton);

            Button button = activeGameButton.getButton();
            button.setOnAction(event ->
                    this.gameController.fireEvent(
                            new JoinToGameEvent(
                                    activeGameButton.getMasterNode(),
                                    activeGameButton.getMasterNodeName(),
                                    activeGameButton.getConfig()
                            )
                    )
            );
        });
        this.gameInfoObservableList.setAll(this.activeGameButtons);
    }

    private void initPlayersInfoTable() {
        this.playersRankingTable.setItems(this.playersObservableList);
        this.playerNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        this.playerScoreColumn.setCellValueFactory(new PropertyValueFactory<>("score"));
    }

    private void initGameListTable() {
        this.gameListTable.setItems(this.gameInfoObservableList);
        this.masterColumn.setCellValueFactory(new PropertyValueFactory<>("masterNodeName"));
        this.foodColumn.setCellValueFactory(new PropertyValueFactory<>("foodNumber"));
        this.playersNumberColumn.setCellValueFactory(new PropertyValueFactory<>("playersNumber"));
        this.fieldSizeColumn.setCellValueFactory(new PropertyValueFactory<>("fieldSize"));
        this.connectButtonColumn.setCellValueFactory(new PropertyValueFactory<>("button"));
    }

    private void setActionOnButtons() {
        this.exitButton.setOnAction(event -> close(false));
        this.newGameButton.setOnAction(event -> this.gameController.fireEvent(new NewGameEvent()));
    }

    private void close(boolean closeStage) {
        if (closeStage) {
            if (null == this.stage) {
                throw new IllegalStateException("Cant close not initialized stage");
            }
            this.stage.close();
        }
        this.gameController.fireEvent(new ExitEvent());


        Platform.runLater(() -> {
            this.foodAmount.setText("");
            this.fieldSize.setText("");
            this.gameOwner.setText("");
        });
        this.playersObservableList.clear();
        for (int row = 0; row < gameConfig.getHeight(); row++) {
            for (int col = 0; col < gameConfig.getWidth(); col++) {
                this.fieldCells[row][col].setFill(EMPTY_CELL_COLOR);
            }
        }
    }

    private Optional<Direction> getDirectionByKeyCode(@NotNull KeyCode code) {
        return switch (code) {
            case UP, W -> Optional.of(Direction.UP);
            case DOWN, S -> Optional.of(Direction.DOWN);
            case RIGHT, D -> Optional.of(Direction.RIGHT);
            case LEFT, A -> Optional.of(Direction.LEFT);
            default -> Optional.empty();
        };
    }

    private void updateField(GameState state) {
        Map<Snake, Color> snakes = createSnakesMap(state);
        for (int row = 0; row < gameConfig.getHeight(); row++) {
            for (int col = 0; col < gameConfig.getWidth(); col++) {
                this.fieldCells[row][col].setFill(EMPTY_CELL_COLOR);
            }
        }
        snakes.forEach((snake, color) ->
                snake.getPoints().forEach(point -> this.fieldCells[point.getY()][point.getX()].setFill(color))
        );
        state.getFruits().forEach(fruit -> this.fieldCells[fruit.getY()][fruit.getX()].setFill(FRUIT_COLOR));
    }

    private void buildField() {
        int gameFieldHeight = this.gameConfig.getHeight();
        int gameFieldWidth = this.gameConfig.getWidth();
        int rectHeight = (int) (this.gameFieldPane.getPrefHeight() / gameFieldHeight);
        int rectWidth = (int) (this.gameFieldPane.getPrefWidth() / gameFieldWidth);
        GridPane gridPane = new GridPane();
        this.fieldCells = new Rectangle[gameFieldHeight][gameFieldWidth];
        for (int row = 0; row < gameFieldHeight; row++) {
            for (int col = 0; col < gameFieldWidth; col++) {
                Rectangle rectangle = new Rectangle(rectWidth, rectHeight, EMPTY_CELL_COLOR);
                this.fieldCells[row][col] = rectangle;
                gridPane.add(rectangle, col, row);
            }
        }
        gridPane.setGridLinesVisible(true);
        this.gameFieldPane.setCenter(gridPane);
    }

    private Map<Snake, Color> createSnakesMap(GameState state) {
        updatePlayersColors(state.getActivePlayers());
        Map<Snake, Color> snakes = new HashMap<>();
        for (var snake : state.getSnakes()) {
            if (snake.getState() == SnakesProto.GameState.Snake.SnakeState.ZOMBIE) {
                snakes.put(snake, this.colorMapper.getZombieSnakeColor());
                continue;
            }
            Color playerColor = this.colorMapper.getColor(Optional.ofNullable(PlayerUtils.findPlayerBySnake(snake, state.getActivePlayers())).orElseThrow()).orElseThrow(() -> new NoSuchElementException("Color map doesn't contain player"));
            snakes.put(snake, playerColor);
        }
        return snakes;
    }

    private void updatePlayersColors(List<Player> players) {
        removeInactivePlayersFromColorMap(players);
        players.forEach(activePlayer -> {
            if (!this.colorMapper.isPlayerRegistered(activePlayer)) {
                this.colorMapper.addPlayer(activePlayer);
            }
        });
    }

    private void removeInactivePlayersFromColorMap(List<Player> players) {
        List<Player> inactiveRegisteredUsers = this.colorMapper.getRegisteredPlayers().stream()
                .filter(registeredPlayer -> !players.contains(registeredPlayer))
                .collect(Collectors.toList());
        inactiveRegisteredUsers.forEach(this.colorMapper::removePlayer);
    }
}
