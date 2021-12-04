package ru.nsu.spirin.snake;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.stage.Stage;
import lombok.Setter;
import lombok.SneakyThrows;
import me.ippolitov.fit.snakes.SnakesProto.GameConfig;
import org.apache.log4j.Logger;
import ru.nsu.spirin.snake.config.ConfigReader;
import ru.nsu.spirin.snake.config.ConfigValidator;
import ru.nsu.spirin.snake.client.view.javafx.JavaFXView;
import ru.nsu.spirin.snake.client.network.GameNetwork;
import ru.nsu.spirin.snake.client.controller.JavaFXController;
import ru.nsu.spirin.snake.datatransfer.RDTSocket;
import ru.nsu.spirin.snake.multicastreceiver.MulticastReceiver;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public final class JavaFXStarter extends Application {
    private static final Logger logger = Logger.getLogger(JavaFXStarter.class);

    private static final String GAME_VIEW_FXML_PATH = "gameView.fxml";
    private static final String MULTICAST_HOST = "239.192.0.4";
    private static final int MULTICAST_PORT = 9192;

    private static @Setter String playerName;

    private MulticastReceiver multicastReceiver = null;
    private GameNetwork gameNetwork = null;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        GameConfig config = ConfigReader.getConfig();
        ConfigValidator.validate(config);

        try {
            InetSocketAddress multicastInfo = new InetSocketAddress(InetAddress.getByName(MULTICAST_HOST), MULTICAST_PORT);

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(JavaFXStarter.class.getClassLoader().getResource(GAME_VIEW_FXML_PATH));
            SplitPane root = loader.load();

            JavaFXView view = loader.getController();
            this.gameNetwork = new GameNetwork(new RDTSocket(new DatagramSocket(), config.getNodeTimeoutMs()), config, playerName, view, multicastInfo);
            JavaFXController gameEventHandler = new JavaFXController(config, playerName, this.gameNetwork, view);

            this.multicastReceiver = new MulticastReceiver(multicastInfo, view);
            this.multicastReceiver.start();

            view.setStage(stage);
            view.setGameController(gameEventHandler);

            stage.setTitle(playerName);
            stage.setScene(new Scene(root));
            stage.sizeToScene();
            stage.show();
        }
        catch (IOException exception) {
            logger.error(exception.getLocalizedMessage());
        }
    }

    @Override
    @SneakyThrows
    public void stop() {
        if (null != this.multicastReceiver) {
            this.multicastReceiver.stop();
        }
        if (null != this.gameNetwork) {
            this.gameNetwork.exit();
        }
    }
}
