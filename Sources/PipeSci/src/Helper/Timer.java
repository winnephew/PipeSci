package Helper;


import java.io.File;
import java.io.*;

public class Timer {

    private long start;
    private String name;
    private String info = "";
    private boolean seconds = false;
    private File file = new File(System.getProperty("user.dir"), "/logs/TIME_" + Log.type + Log.localMachine + Log.timerWorkerExtension);

    public Timer(String timerName){
        name = timerName;
        start = System.nanoTime();
    }
    public Timer(String timerName, String info){
        name = timerName;
        start = System.nanoTime();
        this.info = info;
    }

    public double stop(){
        long end = System.nanoTime();
        long span = end - start;
        double secs = span / 1e9;
        switch(Config.timing) {
            case "ns":
                break;
            case "s":
                span /= 1e9;
                break;
            case "m":
                span /= 1e9 * 60;
                break;
            default:
            case "ms":
                span /= 1e6;
                break;
        }

        try(FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter out = new PrintWriter(bw))
        {
            String time = name + " " + String.valueOf(span) + " " + Config.timing + " " + info + "\n";
            Log.time(time);
            out.append(time);
            out.close();
        } catch (IOException e) { Log.error("Couldn't time: " +e ); }
        return secs;
    }


}
