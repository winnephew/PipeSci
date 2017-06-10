package com.Pipeformance;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

public class Log {

    private final static Logger logger = Logger.getLogger("Log");
    private Handler file_handler;
    private Formatter text;
    public static String localMachine = "";

    public Log(String type) throws IOException {
        logger.setLevel(Level.ALL);

        localMachine = java.net.InetAddress.getLocalHost().getHostName();

        File loggingDirectory = new File(System.getProperty("user.dir") + "/logs");
        loggingDirectory.mkdir();
        loggingDirectory.setWritable(true);

        file_handler = new FileHandler(loggingDirectory + "/LOG_" + type + "_" + localMachine + ".txt");
        text = new SimpleFormatter();
        file_handler.setFormatter(text);
        logger.addHandler(file_handler);
    }

    public static void setLevel(Level level){
        logger.setLevel(level);
    }

    public static void debug(Object msg) {
        logger.info(msg.toString() + "\n");
    }

    public static void info(Object msg) {
        logger.config(msg.toString() + "\n");
    }

    public static void error(Object msg) {
        logger.severe(msg.toString() + "\n");
    }

    public static void time(Object msg) {
        logger.warning(msg.toString() + "\n");
    }

}
