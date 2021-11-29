package ru.nsu.spirin.snake;

import org.apache.log4j.Logger;

public final class Main {
    private static final Logger logger = Logger.getLogger(Main.class);

    public static void main(String[] args) {
        if (1 != args.length) {
            logger.info("Usage: player_name");
            return;
        }

        JavaFXStarter.setPlayerName(args[0]);
        JavaFXStarter.main(args);
    }
}
