package com.Pipeformance;

import java.util.*;
import java.io.*;


public class Sequence {

    public static List<Reader> inputs = Main.inputs;
    public static List<Writer> outputs = Main.outputs;
    public static Algorithms alg = new Algorithms();

    private ArrayList<String> inPart = new ArrayList<String>();
    private ArrayList<String> outPart = new ArrayList<String>();
    private Reader in;
    private Writer out;
    private boolean producer = true;
    private long start = 0;

    public Sequence(){
        start = System.nanoTime();

        if(!inputs.isEmpty()){
            in = inputs.get(0);
            producer = false;
        }

        if(!outputs.isEmpty()){
            out = outputs.get(0);
        }else{
            Log.error("No output file.");
        }
        
        startCalc();
    }

    private void startCalc(){
        int send = 0;
        int bytesSend = 0;
        boolean eof = false;
        double aggrDiff = 0;
        int parts = 0;

        while( (send < Main.units || !producer) && !eof ){

            //long newstart = System.nanoTime();

            if(producer){
                // Produce output without input
                long rest = Main.units - send;
                if(rest < Main.partUnits){
                    outPart = alg.producePart(rest);
                }else{
                    outPart = alg.producePart(Main.partUnits);
                }

            }else{
                // Get input from File
                try{
                    in.mark((int)(Main.partUnits * 32));
                    inPart = new ArrayList<String>();
                    if(!FileAccess.readPart(in, inPart)) {
                        eof = true;
                        Log.debug("EOF");
                    }else{
                        parts ++;

                        outPart = alg.calculatePart(inPart);
                    }
                }catch(Exception e){
                    Log.error("Couldn't receive Bytes: " + e );
                    try{
                        in.reset();
                    }
                    catch(Exception excep){
                        Log.error("Couldn't reset Stream: " + excep);
                    }
                }
            }

            try{
                long[] sendArr = FileAccess.writePart(out,outPart);
                bytesSend += sendArr[1];
                send += sendArr[0];
            }catch (Exception e){
                Log.error("Couldn't send Bytes");
            }

            //double span = (System.nanoTime() - newstart) / 1e9;
            //aggrDiff += span - Main.exectime;
            //Log.debug( "Partition latency: " + span + " seconds.");
        }
        //Main.log.debug("Wrote " + send + " units.");
        try{
            Log.debug( "Parts: " + parts);
            FileAccess.writeNum(out,"EOF ");
            bytesSend += "EOF ".getBytes().length;
        }catch (Exception e){
            Log.error("Couldn't send EOF");
        }

        //Log.debug("Aggregated Difference of Task Delays: " + aggrDiff + "s.");
        Log.debug( "Execution Time: " + ((System.nanoTime() - start)/1e9) + " seconds.");
        System.out.println("Wrote " + bytesSend + " bytes.");
        Log.debug("Wrote " + bytesSend + " bytes.");
        System.exit(0);

    }
}
