package Worker;

import Helper.*;
import Helper.Timer;

import java.io.*;
import java.net.InetAddress;
import java.util.*;
import java.lang.*;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class WorkerExecutor {

    private List<String> command;
    private File taskDirectory;
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private List<String> outputTargets;
    private String outputName;
    private File outputFile;
    private boolean processDone = false;
    private CountDownLatch filesSend;
    private boolean pipelining = false;
    public static Timer taskDelay;
    private double execTime = 0;

    public WorkerExecutor(List<String> command, String outputName, List<String> outputTargets, File taskDirectory, boolean pipelining) {
        this.command = command;
        this.outputName = outputName;
        this.outputTargets = outputTargets;
        filesSend= new CountDownLatch(outputTargets.size());
        this.taskDirectory = taskDirectory;
        this.pipelining = pipelining;
    }

    public String startExec() {
        Log.debug("WorkerExecutor: start Exec - " + command + " - output: " + outputTargets);

        ProcessBuilder builder = new ProcessBuilder(command);

        // Setting Directory for Task
        builder.directory(taskDirectory);

        outputFile = new File(taskDirectory + "/", this.outputName);

        File stdout = new File(taskDirectory, "stdout");
        builder.redirectOutput(ProcessBuilder.Redirect.to(stdout));

        // Logging
        File errorLog = new File(taskDirectory, "errorLog");
        builder.redirectError(ProcessBuilder.Redirect.to(errorLog));

        // Start Program
        try {
            Log.debug("WorkerExecutor: Starting Process");
            Timer time = new Timer("ProcessExecutionTime", command.toString());
            final Process process = builder.start();
            if (pipelining) {
                sendOutput(true);
            }
            process.waitFor();
            execTime = time.stop();
            processDone = true;
            Log.debug("WorkerExecutor: Process done");
            if(!pipelining){
                sendOutput(false);
            }

            filesSend.await();

            return "Task Finished";
        } catch (Exception e) {
            Log.error("WorkerExecutor - Error when starting Process: " + e);
            return "Task Failed";
        }
    }

    private InputStreamBuffer inBuffer;
    private void sendOutput(boolean stream){
        Log.debug("Output Targets: " + outputTargets);

        try{
            inBuffer = new InputStreamBuffer(new FileInputStream(outputFile), outputTargets.size());
        }
        catch(Exception e){
            Log.error("Could not prepare InputStreamBuffer:" + e);
        }

        int readerNum = 0;
        for(String target: outputTargets){
            Log.error("Output Target: " + target);
            String[] targetSplit = target.split(":");
            String outputTarget = "";
            int outputPort = 0;
            switch (targetSplit[0]){
                case "self":
                    // TODO:  Save File and use Later, sth like deactivate Pipelining?????
                    break;
                case "manager":
                    outputTarget = Worker.mip;
                    outputPort = Config.basePort+1;
                    break;
                default:
                    outputTarget = targetSplit[0];
                    outputPort = Integer.parseInt(targetSplit[1]);
                    break;
            }

            if(stream){
                streamToNext(outputTarget,outputPort, readerNum);
                readerNum ++;
            }else{
                sendToNext(outputTarget,outputPort);
            }
        }
    }


    private void streamToNext(final String outputTarget, final int outputPort, final int readerNum) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try (TCPSocket nextSocket = new TCPSocket(outputTarget, outputPort)) {
                        nextSocket.streamFileHeader(outputName);
                        long bytesSend = 0;
                        while(!processDone && !nextSocket.streamFileEnded){
                            bytesSend += nextSocket.streamFile(inBuffer, readerNum, outputName);
                        }
                        Log.debug(("OutputName: " + outputName + " OutputTarget: " + outputTarget + " BytesSend: " + bytesSend + " ExecTimeSecs: " + execTime));
                        Log.toTimerFile("Throughput-Bytes_s", ((bytesSend) / execTime));
                        Log.debug("Worker Executor done streaming File: " + outputFile.getName());
                        filesSend.countDown();
                        nextSocket.close();
                        return;
                    } catch (Exception e) {
                        Log.error("Worker Executor: Trying to Stream Output to next worker - " + outputTarget+ ":" + outputPort + " - " + e );
                    }
                }
            }
        });
    }

    private void sendToNext(final String outputTarget, final int outputPort) {
        executorService.submit( new Runnable(){
           @Override
            public void run(){
               while(true){
                   try (TCPSocket nextSocket = new TCPSocket(outputTarget, outputPort)) {
                       PacketBuilder.taskDelay.stop();  // Timer: first Output Bytes
                       long bytesSend = nextSocket.sendFile(outputFile);
                       Log.toTimerFile("Throughput-Bytes_s", ((bytesSend) / execTime));
                       Log.debug("File send: " + outputFile.getName());
                       filesSend.countDown();
                       nextSocket.close();
                       return;
                   } catch (Exception e) {
                       Log.error("Konnte output nicht weiterleiten:" + outputFile.getName() +  " to " + outputTarget + " " + e);
                   }
               }
           }
        });
    }

}

