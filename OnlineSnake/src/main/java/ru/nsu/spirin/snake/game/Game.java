package ru.nsu.spirin.snake.game;

import lombok.Getter;
import me.ippolitov.fit.snakes.SnakesProto.Direction;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import me.ippolitov.fit.snakes.SnakesProto.GameState.Snake.SnakeState;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.nsu.spirin.snake.datatransfer.NetNode;
import ru.nsu.spirin.snake.utils.PlayerUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Game implements GameObservable {
    private static final Logger logger = Logger.getLogger(Game.class);

    private static final int SIZE_OF_EMPTY_SQUARE_FOR_SNAKE = 5;
    public static final String UNKNOWN_PLAYER_ERROR_MESSAGE = "Unknown player";

    private final @Getter Map<Player, Snake> playersWithSnakes = new HashMap<>();
    private final Map<Player, Snake> playersForRemove = new HashMap<>();
    private final List<Snake> zombieSnakes = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private final GameConfig config;
    private final List<Cell> fruits;
    private final GameField field;
    private final ArrayList<GameObserver> gameObservers;
    private final Random random = new Random();
    private int stateID;

    private int playerIDCounter = 1;

    public Game(@NotNull GameConfig config) {
        this.config = Objects.requireNonNull(config, "Config cant be null");
        field = new GameField(config.getWidth(), config.getHeight());
        gameObservers = new ArrayList<>();
        stateID = 0;
        fruits = new ArrayList<>(config.getFoodStatic());
        generateFruits();
    }

    public Game(@NotNull GameState state) {
        config = state.getGameConfig();
        field = new GameField(config.getWidth(), config.getHeight());
        stateID = state.getStateID();
        gameObservers = new ArrayList<>();
        List<Snake> snakes = state.getSnakes();
        snakes.forEach(snake -> {
            markSnakeOnField(snake);
            if (snake.getState() == SnakeState.ZOMBIE) {
                zombieSnakes.add(snake);
            } else {
                Player snakeOwner = Optional.ofNullable(PlayerUtils.findPlayerBySnake(snake, state.getActivePlayers()))
                        .orElseThrow(
                                () -> new IllegalStateException("Cant get player from alive snake")
                        );
                playersWithSnakes.put(snakeOwner, snake);
            }
        });
        players.addAll(state.getActivePlayers());
        fruits = new ArrayList<>(state.getFruits().size());
        state.getFruits().forEach(fruit -> {
            field.set(fruit, CellType.FRUIT);
            fruits.add(new Cell(fruit, CellType.FRUIT));
        });
    }

    private void markSnakeOnField(Snake snake) {
        for (Point2D snakePoint : snake.getPoints()) {
            field.set(snakePoint, CellType.SNAKE);
        }
    }

    @NotNull
    public Player registrationNewPlayer(@NotNull String playerName, NetNode netNode) {
        Player player = new Player(playerName, this.playerIDCounter, netNode);
        this.playerIDCounter++;
        List<Cell> headAndTailOfNewSnake = getNewSnakeHeadAndTail();
        if (headAndTailOfNewSnake.isEmpty()) {
            throw new IllegalStateException("Cant add new player because no space on field");
        }
        Snake playerSnake = new Snake(
                headAndTailOfNewSnake.get(0).getPoint(),
                headAndTailOfNewSnake.get(1).getPoint(),
                field.getWidth(),
                field.getHeight()
        );
        playerSnake.setPlayerID(player.getId());
        headAndTailOfNewSnake.forEach(cell -> field.set(cell.getY(), cell.getX(), CellType.SNAKE));
        players.add(player);
        playersWithSnakes.put(player, playerSnake);
        return player;
    }

    private List<Cell> getNewSnakeHeadAndTail() {
        Optional<Cell> centerOfEmptySquareOnField = field.findCenterOfSquareWithOutSnake(SIZE_OF_EMPTY_SQUARE_FOR_SNAKE);
        if (centerOfEmptySquareOnField.isEmpty()) {
            return Collections.emptyList();
        }
        Cell snakeHead = centerOfEmptySquareOnField.get();
        Optional<Cell> snakeTail = findTailWithoutFruit(snakeHead);
        if (snakeTail.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(snakeHead, snakeTail.get());
    }

    private Optional<Cell> findTailWithoutFruit(Cell head) {
        return Stream.of(
                field.get(head.getY() - 1, head.getX()),
                field.get(head.getY() + 1, head.getX()),
                field.get(head.getY(), head.getX() - 1),
                field.get(head.getY(), head.getX() + 1)
        )
                .filter(cell -> cell.getType() == CellType.EMPTY)
                .findFirst();
    }

    public void removePlayer(@NotNull Player player) {
        Objects.requireNonNull(player, "Player cant be null");
        if (!playersWithSnakes.containsKey(player)) {
            return;
        }
        Snake snake = playersWithSnakes.get(player);
        snake.setState(SnakeState.ZOMBIE);
        zombieSnakes.add(snake);
        markPlayerInactive(player);
    }

    private void markPlayerInactive(@NotNull Player player) {
        playersWithSnakes.remove(player);
        players.remove(player);
    }

    private void makeMove(@NotNull Player player, @Nullable Direction direction) {
        Objects.requireNonNull(player, "Player cant be null");
        if (!playersWithSnakes.containsKey(player)) {
            throw new IllegalArgumentException(UNKNOWN_PLAYER_ERROR_MESSAGE);
        }
        Snake snake = playersWithSnakes.get(player);
        if (direction == null) {
            snake.makeMove();
        } else {
            snake.makeMove(direction);
        }
        if (isSnakeCrashed(playersWithSnakes.get(player))) {
            handlePlayerLose(player, snake);
            return;
        }
        if (isSnakeAteFruit(snake)) {
            incrementScore(player);
            removeFruit(snake.getHead());
        } else {
            field.set(snake.getTail(), CellType.EMPTY);
            snake.removeTail();
        }
        field.set(snake.getHead(), CellType.SNAKE);
    }

    private void removeFruit(Point2D fruitForRemove) {
        fruits.removeIf(fruit -> fruitForRemove.equals(fruit.getPoint()));
    }

    private void handlePlayerLose(Player player, Snake playerSnake) {
        playersForRemove.put(player, playerSnake);
    }

    public GameConfig getConfig() {
        return config;
    }

    public void makeAllPlayersMove(@NotNull Map<Player, Direction> playersMoves) {
        playersWithSnakes
                .keySet()
                .forEach(
                        player -> makeMove(player, playersMoves.getOrDefault(player, null))
                );
        zombieSnakesMove();
        generateFruits();
        playersForRemove
                .keySet()
                .forEach(player -> {
                    makeFruitsFromSnakeWithProbability(playersWithSnakes.get(player));
                    markPlayerInactive(player);
                });
        playersForRemove.clear();
        notifyObservers();
    }

    private void zombieSnakesMove() {
        zombieSnakes.forEach(this::zombieMove);
        zombieSnakes.stream()
                .filter(this::isSnakeCrashed)
                .forEach(this::makeFruitsFromSnakeWithProbability);
        zombieSnakes.removeIf(this::isSnakeCrashed);
    }

    private void zombieMove(Snake snake) {
        snake.makeMove();
        if (isSnakeAteFruit(snake)) {
            removeFruit(snake.getHead());
        } else {
            field.set(snake.getTail(), CellType.EMPTY);
            snake.removeTail();
        }
        field.set(snake.getHead(), CellType.SNAKE);
    }

    private void generateFruits() {
        int aliveSnakesCount = playersWithSnakes.size();
        int requiredFruitsNumber = config.getFoodStatic() + (int) (config.getFoodPerPlayer() * aliveSnakesCount);
        if (fruits.size() == requiredFruitsNumber) {
            return;
        }
        if (field.getEmptyCellsNumber() < requiredFruitsNumber) {
            logger.debug("Cant generate required number of fruits=" + requiredFruitsNumber + ", empty cells number=" + field.getEmptyCellsNumber());
            return;
        }
        while (fruits.size() < requiredFruitsNumber) {
            Cell randomEmptyCell = field.findRandomEmptyCell()
                    .orElseThrow(() -> new IllegalStateException("Cant find empty cell"));
            field.set(randomEmptyCell.getPoint(), CellType.FRUIT);
            fruits.add(randomEmptyCell);
        }
    }

    private void incrementScore(Player player) {
        if (!players.contains(player)) {
            throw new IllegalArgumentException(UNKNOWN_PLAYER_ERROR_MESSAGE);
        }
        player.setScore(player.getScore() + 1);
    }

    private boolean isSnakeAteFruit(Snake snake) {
        Point2D snakeHead = snake.getHead();
        return fruits.stream()
                .anyMatch(
                        fruit -> snakeHead.equals(fruit.getPoint())
                );
    }

    private void makeFruitsFromSnakeWithProbability(Snake snake) {
        for (Point2D p : snake.getPoints()) {
            if (p.equals(snake.getHead())) {
                continue;
            }
            if (random.nextDouble() < config.getDeadFoodProb()) {
                field.set(p, CellType.FRUIT);
                fruits.add(field.get(p.getY(), p.getX()));
            } else {
                field.set(p, CellType.EMPTY);
            }
        }
    }

    private boolean isSnakeCrashed(Snake snake) {
        if (isSnakeCrashedToZombie(snake)) {
            return true;
        }
        for (Map.Entry<Player, Snake> playerWithSnake : playersWithSnakes.entrySet()) {
            Snake otherSnake = playerWithSnake.getValue();
            if (checkCrashIntoYourself(snake)) {
                return true;
            }
            if (otherSnake != snake && otherSnake.isSnake(snake.getHead())) {
                incrementScore(playerWithSnake.getKey());
                return true;
            }
        }
        return false;
    }

    private boolean isSnakeCrashedToZombie(Snake snake) {
        return zombieSnakes.stream()
                .anyMatch(zombieSnake ->
                        zombieSnake != snake && zombieSnake.isSnake(snake.getHead())
                );
    }

    private boolean checkCrashIntoYourself(Snake snake) {
        return snake.isSnakeBody(snake.getHead()) || snake.getTail().equals(snake.getHead());
    }

    @Override
    public void addObserver(GameObserver gameObserver) {
        gameObservers.add(gameObserver);
        notifyObservers();
    }

    @Override
    public void removeObserver(GameObserver gameObserver) {
        gameObservers.remove(gameObserver);
    }


    @Override
    public void notifyObservers() {
        GameState gameState = generateGameState();
        for (GameObserver gameObserver : gameObservers) {
            gameObserver.update(gameState);
        }
    }

    private GameState generateGameState() {
        int currentStateID = this.stateID++;
        return new GameState(
                getFruitsAsPointsList(),
                generatePlayersWithTheirScoresList(),
                generateSnakeList(),
                config,
                currentStateID
        );
    }

    @NotNull
    private List<Point2D> getFruitsAsPointsList() {
        return fruits.stream()
                .map(Cell::getPoint)
                .collect(Collectors.toList());
    }

    @NotNull
    private List<Snake> generateSnakeList() {
        List<Snake> snakes = new ArrayList<>(playersWithSnakes.size() + zombieSnakes.size());
        playersWithSnakes.forEach((player, snake) -> snakes.add(snake));
        snakes.addAll(zombieSnakes);
        return snakes;
    }

    @NotNull
    private List<Player> generatePlayersWithTheirScoresList() {
        return new ArrayList<>(this.players);
    }
}
