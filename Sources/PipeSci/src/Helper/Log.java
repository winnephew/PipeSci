package Helper;

import java.io.*;
import java.util.logging.*;

public class Log {

    private final static Logger logger = Logger.getLogger("Log");
    private Handler file_handler;
    private Formatter text;
    public static String localMachine = "";
    public static long timerWorkerExtension;
    public static String type = "";
    private static File timerFile;

    public Log(String instanceType) throws IOException {
        type = instanceType;
        logger.setLevel(Config.loggingLevel);

        localMachine = java.net.InetAddress.getLocalHost().getHostName();

        File loggingDirectory = new File(System.getProperty("user.dir") + "/logs");
        loggingDirectory.mkdir();
        loggingDirectory.setWritable(true);

        timerWorkerExtension = System.nanoTime();
        timerFile = new File(System.getProperty("user.dir"), "/logs/TIME_" + type + localMachine + timerWorkerExtension);
        while(timerFile.exists()){
            timerWorkerExtension +=1;
            timerFile = new File(System.getProperty("user.dir"), "/logs/TIME_" + type + localMachine + timerWorkerExtension);
        }
        timerFile.createNewFile();

        file_handler = new FileHandler(loggingDirectory + "/LOG_" + type + "_" + localMachine + timerWorkerExtension + ".txt");
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
        logger.warning("Timer: " + msg.toString() + "\n");
    }

    public static void toTimerFile(Object name, Object msg ) {
        try(FileWriter fw = new FileWriter(timerFile, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            String time = name.toString() + " " + msg.toString() + "\n";
            Log.time(time);
            out.append(time);
            out.close();
        } catch (IOException e) { Log.error("Log: Couldn't write to timerFile: " +e ); }
    }

}
