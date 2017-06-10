package com.Pipeformance;

import java.io.*;
import java.util.*;

public class Reduce {

    public static List<Reader> inputs;
    public static List<Writer> outputs;
    private Writer out;
    public static Algorithms alg = new Algorithms();
    public HashMap<Integer, Boolean> eofs;
    private long bytesSend = 0;

    private ArrayList<String> inPart = new ArrayList<String>();
    private ArrayList<String> outPart = new ArrayList<String>();
    private int partCount = 0;
    private long start = 0;

    public Reduce(){
        start = System.nanoTime();
        int parts = 0;
        inputs = Main.inputs;
        outputs = Main.outputs;
        out = outputs.get(0);
        eofs = new HashMap<Integer, Boolean>();
        for(Reader input : inputs){
            eofs.put(input.hashCode(), false);
        }


        while(eofs.containsValue(false)){

            inPart = new ArrayList<String>();

            while( partCount < Main.partUnits ) {
                for (Reader input : inputs) {
                    // if EOF of this file has been reached, skip it
                    if (eofs.get(input.hashCode())) {
                        continue;
                    }
                    read(input);
                    if(partCount == Main.partUnits){break;}
                }
                if(!eofs.containsValue(false)){break;}
            }

            partCount = 0;
            outPart = inPart;
            if(eofs.containsValue(false)) {
                parts++;
                outPart = alg.calculatePart(inPart);
            }

            try{
                long[] sendArr = FileAccess.writePart(out,outPart);
                bytesSend += sendArr[1];
            }catch (Exception e){
                System.out.println("Couldn't send Bytes");
            }
        }

        try{
            FileAccess.writeNum(out,"EOF ");
            bytesSend += "EOF ".getBytes().length;
        }catch (Exception e){
            Log.error("Couldn't send EOF");
        }
        Log.debug( "Execution Time: " + ((System.nanoTime() - start)/1e9) + " seconds.");
        Log.debug( "Parts: " + parts);
        System.out.println("Wrote " + bytesSend + " bytes.");
        Log.debug("Wrote " + bytesSend + " bytes.");
        System.exit(0);
    }


    private void read(Reader input){
        try{
            input.mark(32);
            String numStr = FileAccess.readNum(input);
            if(numStr.equals("EOF")){ eofs.put(input.hashCode(), true); return;}
            inPart.add(numStr);
            partCount ++;
        }
        catch(Exception e){
            System.out.println("Couldn't receive Bytes: " + e );
            try{
                input.reset();
            }
            catch(Exception excep){
                System.out.println("Couldn't reset Stream: " + excep);
            }
        }
    }



/*

    private void consume(Reader input){
        String outputData = "";
        try{
            input.mark(64);
            String numStr1 = FileAccess.readNum(input);
            if(numStr1.equals("EOF")){ eofs.put(input.hashCode(), true); return; };
            String numStr2 = FileAccess.readNum(input);
            if(numStr2.equals("EOF")){ eofs.put(input.hashCode(), true); return; };
            outputData = alg.consume(Long.parseLong(numStr1), Long.parseLong(numStr2));
        }
        catch(Exception e){
            System.out.println("Couldn't receive Bytes: " + e );
            try{
                input.reset();
            }
            catch(Exception excep){
                System.out.println("Couldn't reset Stream: " + excep);
            }

        }
        try{
            FileAccess.writeNum(out,outputData);
            bytesSend += outputData.getBytes().length;
        }catch (Exception e){
            System.out.println("Couldn't send Bytes");
        }
    }
    */

}
