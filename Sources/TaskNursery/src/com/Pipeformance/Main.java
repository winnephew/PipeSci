package com.Pipeformance;
import java.io.*;
import java.util.*;

public class Main {

    public static List<Reader> inputs = new ArrayList<Reader>();
    public static List<Writer> outputs = new ArrayList<Writer>();
    public static long units = 5;
    public static long partUnits = 1;
    public static long exectime = 1; // in seconds

    private enum States{
        None, Input, Output, Mb, Partsize, Exectime
    }
    private enum Modes{
        Sequence, Reduce
    }

    public static void main(String[] args) {
        try{
            Log log = new Log("TaskNursery");
        }
        catch(Exception e){
            System.out.println("Couldn't ini logger.");
        }

        States state = States.None;
        Modes mode = Modes.Sequence;

        for(String s: args) {
            switch(s){
                case "help":
                    Log.debug(manual);
                case "-input":
                    state = States.Input;
                    break;
                case "-output":
                    state = States.Output;
                    break;
                case "-mb":
                    state = States.Mb;
                    break;
                case "-partsize":
                    state = States.Partsize;
                    break;
                case "-partdelay":
                    state = States.Exectime;
                    break;
                case "--sequence":
                    state = States.None;
                    mode = Modes.Sequence;
                    break;
                case "--reduce":
                    state = States.None;
                    mode = Modes.Reduce;
                    break;
                default:
                    switch(state){
                        case Input:
                            File in = new File(s);
                            if(!in.exists()){
                                in = new File("../" + s);
                            }
                            if(in.exists() && !in.isDirectory()){
                                try{
                                    Reader fin = new BufferedReader(new InputStreamReader (new FileInputStream(in), "UTF-8"));
                                    inputs.add(fin);
                                }catch (Exception e){
                                    System.out.println("Could not open InputStream: " + s + " - " +e );
                                    Log.debug("Could not open InputStream: " + s + " - " +e );
                                }
                            }else{
                                Log.debug("File does not exist: " + s);
                                return;
                            }
                            break;
                        case Output:
                            File out = new File(s);
                            if(!out.exists()){
                                try {
                                    // should only happen if we're not pipelining, framework will create fifos
                                    out.createNewFile();
                                }
                                catch(Exception e){
                                    System.out.println("File didn't exist and couldn't create file: " + s + " - " +e );
                                    Log.debug("File didn't exist and couldn't create file: " + s + " - " +e );
                                }
                            }
                            if(out.exists() && !out.isDirectory()){
                                try{
                                    Writer fout = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), "UTF-8"));
                                    outputs.add(fout);
                                }catch (Exception e){
                                    System.out.println("Could not open OutputStream: " + s + " - " +e );
                                    Log.debug("Could not open OutputStream: " + s + " - " +e );
                                }
                            }else{
                                System.out.println("File does not exist: " + s);
                                Log.debug("File does not exist: " + s);
                                return;
                            }
                            break;
                        case Mb:
                            float megabytes = Float.parseFloat(s);
                            units = (long) (((megabytes * 1000000) / 11) + (megabytes * 166));     // ca. 11 Bytes per Unit
                            break;
                        case Partsize:
                            float psize = Float.parseFloat(s);
                            partUnits = (long) (((psize * 1000000) / 11) + (psize * 166));     // ca. 11 Bytes per Unit
                            //Log.debug(partUnits*11);
                            break;
                        case Exectime:
                            exectime = Long.parseLong(s);
                            break;
                        case None:
                        default:
                            System.out.println("Argument invalid: " + s);
                            Log.debug("Argument invalid: " + s);
                            return;
                    }
            }
        }

        if(partUnits > units && inputs.isEmpty()){
            Log.debug("Size of parts has to be smaller than overall input size (try setting -mb to a higher or -partsize to a lower value).");
            return;
        }
        //Log.debug("Parts size: " + partUnits + " units");

        switch (mode){
            case Reduce:
                System.out.println("Starting Reduce.");
                Log.debug("Starting Reduce.");
                Reduce reduce = new Reduce();
                break;
            case Sequence:
            default:
                System.out.println("Starting Sequence.");
                Log.debug("Starting Sequence.");
                Sequence sequence = new Sequence();
                break;
        }

    }

    private static String manual = "" +
            "Modes/Workflow Patterns:" +
            "\n--sequence: zero/one Input, one Output (default)" + //"\n--broadcast: one Input, multiple Outputs" +
            "\n--reduce: multiple Inputs, one Output" +

            "\n\nOptions:" +
            "\n-input [File]*" +
            "\n-output [File]*" +
            "\n-mb [x]: make the application produce x MB of output data (Default: 5 MB) -> this only has impact in sequence mode and if no input file is provided" +
            "\n-partsize [x]: atomic amount of data the program will execute on (size in MB) - i. e. if you input 5, the program will have to read 5 MB of data before produces output (default: 1 MB) " +
            "\n-partdelay [x]: amount of seconds the program will calculate from the point of receiving the whole partsize before outputting data (default: 1 second)" +
            "\n\nExample: java -jar TaskNursery --Sequence -p -input [File] -output [File]" +
            "\n\nSequences without Input will act as Producers.";
}
