package Helper;

import java.io.IOException;
import java.util.logging.Level;
import java.util.Properties;
import java.io.FileInputStream;

public class Config {
    // use Pipelining or not
    static public String pipeliningSource = "workflow";
    static public boolean pipelining = false;

    // how times are being measure: ms, ns, s
    static public String timing = "ms";

    // Size of the Socket Buffers
    static public int stdBuffSize = 1024;

    // For how many workers we are gonna wait before starting the workflow
    static public int waitWorkers = 3;

    // Which Logging Information we want to catch: SEVERE; WARNING; INFO; CONFIG; FINE; FINER; FINEST
    static public Level loggingLevel = Level.ALL;

    static public int basePort = 4500;

    public Config(){
        try{
            Properties mainProperties = new Properties();
            FileInputStream file;
            String path = "./config.properties";
            file = new FileInputStream(path);
            mainProperties.load(file);
            file.close();

            pipeliningSource = (mainProperties.getProperty("pipelining") != null)? mainProperties.getProperty("pipelining") : pipeliningSource;
            if(pipeliningSource.equals("on")){
                pipelining = true;
            }else if(pipeliningSource.equals("off")) {
                pipelining = false;
            }

            timing = (mainProperties.getProperty("timing") != null)? mainProperties.getProperty("timing") : timing;
            stdBuffSize = (mainProperties.getProperty("stdBuffSize") != null )? Integer.parseInt(mainProperties.getProperty("stdBuffSize")) : stdBuffSize;
            waitWorkers = (mainProperties.getProperty("waitWorkers") != null)? Integer.parseInt(mainProperties.getProperty("waitWorkers")) : waitWorkers;

            loggingLevel = (mainProperties.getProperty("loggingLevel") != null)? Level.parse(mainProperties.getProperty("loggingLevel")) : loggingLevel;
            Log.setLevel(loggingLevel);

            basePort = (mainProperties.getProperty("basePort") != null )? Integer.parseInt(mainProperties.getProperty("basePort")) : basePort;

            Log.debug("Configurations: pipelining=" + pipelining + ", timing=" + timing + ", stdBuffSize=" + stdBuffSize + " waitWorkers=" + waitWorkers
                    + " loggingLevel=" + loggingLevel);

        }catch(Exception e){
            Log.debug("No config.properties found - using standard configuration."  + e);
        }


    }
}