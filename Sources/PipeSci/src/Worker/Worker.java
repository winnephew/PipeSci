package Worker;

import Helper.*;
import java.io.File;
import java.lang.*;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import Helper.Log;


public class Worker {

    private boolean run = true;
    public static String mip;
    private List<String> command;
    private String outputName;
    private List<String> inputNames = new ArrayList<String>();
    private List<String> outputTargets = new ArrayList<String>();
    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Future<String> future;
    private AtomicBoolean once = new AtomicBoolean(true);
    private boolean pipelining = false;

    public File taskDirectory;
    private int taskNum = 0;


    public Worker(String masterIpAddress) {

        mip = masterIpAddress;
        // Start the Main Loop to manage Messaging between all Participants
        mainLoop();
    }

    private void mainLoop() {

        try(TCPSocket tcpSocket = new TCPSocket(mip, Config.basePort)){

            // 1. Connect to Master and register
            Log.debug("Registering at Manager");
            tcpSocket.sendMessage("Register", "");

            while(run){
                try{
                    if( future!= null && future.isDone()){
                        String futureResult = future.get();
                        Log.debug("Task: " +command + " - "+ futureResult);
                        tcpSocket.sendMessage(futureResult, command);

                        if(futureResult.equals("Output not Forwarded")){
                            Log.debug("Output wird hilfsweise an Manager zurückgeleitet");
                            tcpSocket.sendFile(new File(taskDirectory, "/" + outputName));
                        }
                        future = null;
                    }
                }catch(Exception e){
                    Log.error("Could not check future!!!");
                }

                // receive from Master
                try {
                    Packet p = tcpSocket.receivePacket();
                    switch(p.type){
                        case "Command":
                            command = p.command;
                            Log.debug(p.command);

                            // Create an unique Task Folder for the Command
                            String comm = "";
                            for(String s: p.command){
                                comm += " " + s.replace(".","_");
                            }
                            taskDirectory = new File(System.getProperty("user.dir"), "Task" + taskNum + "_" + comm.replace(" ", "_").replace("-",""));
                            taskDirectory.mkdir();
                            taskDirectory.setWritable(true);
                            taskNum++;

                            // Copy the Program into our Task Folder
                            String programName = (p.command.get(0).equals("java"))? p.command.get(2) : p.command.get(0);
                            File program = new File(System.getProperty("user.dir") + "/" + programName);
                            Path programPath = program.toPath();
                            File newProgram = new File(taskDirectory + "/" + programName);
                            Path newProgramPath = newProgram.toPath();
                            try {
                                Files.copy(programPath, newProgramPath, StandardCopyOption.REPLACE_EXISTING);
                                newProgram.setExecutable(true);
                            }
                            catch(Exception e){
                                Log.error("Could not copy Program into TaskDirectory: " +e);
                            }

                            tcpSocket.inputDirectory = taskDirectory;
                            break;

                        case "Pipelining":
                            pipelining = Boolean.parseBoolean(p.message);
                            break;

                        case "FileNames":
                            String[] names = p.message.split(";");
                            Integer inputNum = Integer.parseInt(names[0]);
                            if(inputNum > 0){
                                for(int i = 0; i < inputNum; i++){
                                    this.inputNames.add(names[i+1]);
                                }
                            }
                            outputName = names[inputNum + 1];

                            if(pipelining){
                                createFIFOs();
                            }

                            executorTryStart();
                            break;

                        case "OutputTargets":
                            outputTargets = p.command;
                            break;

                        case "File":
                            Log.debug("File received from Manager: " + p.message);
                            if(inputNames.contains(p.message)){
                                inputNames.remove(p.message);
                            }
                            executorTryStart();
                            break;

                        case "Open Server":
                            final int port = Integer.parseInt(p.message);
                            openServer(port);
                            break;

                        case "Terminate":
                            run = false;
                            executorShutdown();
                            break;
                        default:
                            //Log.error("Worker: Packet not known: " + p.type);
                            break;
                    }
                }
                catch(Exception e){
                    //Log.info("No package received." + e);
                }

            }
            tcpSocket.close();
            System.exit(0);
        }
        catch(Exception e)
        {
            Log.error("Worker - Connection could not be established - Error: " + e);
        }
    }

    private void executorTryStart(){
        // In case of pipelining: Check if all files have been send
        if(!pipelining && inputNames.isEmpty() || pipelining){
            if(command != null && outputName != null && once.compareAndSet(true,false)){
                future = executorService.submit(new Callable<String>(){

                    public String call() throws Exception {
                        try{
                            WorkerExecutor exec;
                            exec = new WorkerExecutor(command, outputName, outputTargets, taskDirectory, pipelining);
                            return exec.startExec();
                        }
                        catch(Exception e){
                            String threadName = Thread.currentThread().getName();
                            Log.error("Worker - ExecutorService Error: " + threadName + " - " + e);
                            return "Task Failed";
                        }
                    }
                });
            }
        }
    }

    private void executorShutdown(){
        try {
            //Log.info("Attempting to shutdown ExecutorService.");
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        }
        catch (InterruptedException e) {
            Log.error("Executor Shutdown: Tasks are being interrupted.");
        }
        finally {
            if (!executorService.isTerminated()) {
                //Log.error("Executor Shutdown: Canceled non-finished Tasks.");
            }
            executorService.shutdownNow();
            //Log.info("ExecutorService shutdown finished");
        }
    }

    private void openServer(final int port){
        executorService.submit( new Runnable() {

            @Override
            public void run() {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    while (run) {
                        Log.debug("Worker - Server Socket " +  port + " opened: Waiting for Connections");
                        try {
                            TCPSocket s = new TCPSocket(serverSocket.accept());
                            s.inputDirectory = taskDirectory;
                            Log.debug("Worker - Server Socket " +  port + ": New Connection" + s.getConnectionHandle());
                            executorService.submit( new WorkerReceiverSlave(s) );
                        } catch (Exception e) {
                            Log.error("Worker "+  port + ": Error when creating connection: " + e);
                        }
                    }
                } catch (Exception exc) {
                    Log.error("Worker "+  port + ": Error when creating ServerSocket" + exc);
                }
            }
        });
    }

    public void createFIFOs(){
        for(String filename: inputNames){
            mkFIFO( taskDirectory + "/" + filename);
        }
        mkFIFO( taskDirectory + "/" + outputName);
    }

    private void mkFIFO(String path){
        try{
            Log.debug("mkfifo: " + path);
            new ProcessBuilder("mkfifo", path).start();
        }
        catch(Exception e){
            Log.error("Could not make FIFO: " + path);
        }
    }


    private class WorkerReceiverSlave implements Runnable {

        private TCPSocket s;

        private WorkerReceiverSlave(TCPSocket s){
            this.s = s;
        }

        @Override
        public void run(){
            boolean keepgoing = true;
            while (run && keepgoing) {
                try {
                    Packet packet = s.receivePacket();
                    switch (packet.type) {
                        case "File":
                            if (inputNames.contains(packet.message)) {
                                inputNames.remove(packet.message);
                            }
                            executorTryStart();
                            break;
                        case "File Stream":
                            if (inputNames.contains(packet.message)) {
                                inputNames.remove(packet.message);
                            }
                            Log.error("Received File Stream: " + packet.message + " InputNames: " + inputNames.toString());
                            executorTryStart();
                            break;
                        default:
                            //Log.error("Worker as Server - Unknown Packet: " + packet.type + " " + packet.message);
                            Thread.sleep(100);
                            break;
                    }
                } catch (Exception e) {
                    Log.error("Worker : Error when trying to receive Packet: " + e);
                    keepgoing = false;
                }
            }
            try { s.close(); }
            catch (Exception e){
                Log.error("Could not close socket" +e );
            }
        }
    }
}
