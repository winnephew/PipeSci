package com.Pipeformance;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Random;
import java.util.Collections;
import java.util.*;
import java.math.BigInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Algorithms {

    private long seed = 1;
    private Random rnd = new Random(seed);

    public Algorithms(){

    }

    public String produce(){
        return String.valueOf(rnd.nextInt()) + " ";
    }

    public String consume( long num1, long num2){
        return String.valueOf(num1) + " ";
        //return String.valueOf(num1 + num2) + " ";
    }

    public ArrayList<String> producePart(long number){

        ArrayList<String> prod = new ArrayList<String>();
        int counter = 0;
        while(counter < number){
            prod.add(String.valueOf(rnd.nextInt()));
            counter++;
        }
        return prod;
    }

    public ArrayList<String> calculatePart(ArrayList<String> inPart){
        /*ArrayList<String> outPart = new ArrayList<String>();

        for(String data : inPart){
            outPart.add(data);
        }
        BigInteger sum = new BigInteger("0");

        // Calc Average and replace last Number in Part with average.
        for (String data : outPart) {
            try{
                sum = sum.add( new BigInteger(data) );
            }catch(Exception e){
                Log.error(data + " error: " + e);
            }
        }
        if(outPart.size() > 0){
            Long avg = sum.divide( BigInteger.valueOf(outPart.size()) ).longValue();
            outPart.remove(outPart.size()-1);
            outPart.add(String.valueOf(avg));
        }

        // Sort
        Collections.sort(outPart);

        return outPart; */


       /*** Variante 2
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        taskExecutor.execute(new MyTask());
        taskExecutor.shutdown();
        try {
            taskExecutor.awaitTermination(Main.exectime, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            // will always be interrupted
        }
        finally {
            if (!taskExecutor.isTerminated()) {
            }
            taskExecutor.shutdownNow();
        }

        **/

        try {
            Thread.sleep(Main.exectime * 1000);
        }
        catch (Exception e){
            Log.error("Could not sleep");
        }

        return inPart;
    }


/**
    class MyTask implements Runnable
    {
        public void run() {

            while(true) {
                ArrayList<Integer> nums = new ArrayList<Integer>();

                while (nums.size() < 1000) {
                    nums.add(rnd.nextInt());
                }

                long sum = 0;

                for (Integer num : nums) {
                    sum += num;
                }

                long avg = sum / nums.size();

            }
        }
    }

 **/


}
