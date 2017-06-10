package Manager;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import Helper.Config;
import Helper.Log;


public class Task {

    private String id;
    private List<String> command;
    private List<String> inputFileNames;
    private String outputFileName;
    private List<String> parents = new ArrayList<>();
    private List<String> children = new ArrayList<>();

    private WorkerHandle worker;
    private boolean done = false;
    private boolean inProgress = false;
    public boolean ready = false;
    private boolean isTask = true;
    private boolean failed = false;
    private List<InetSocketAddress> outputIpAndPort = new ArrayList<>();
    private int inputServerPort;
    private boolean lastTask = false;
    private boolean pipelining;

    public Task(){
        // Constructor only used for empty task
        isTask = false;
    }

    public Task(String taskID, String programName, List<String> commands, List<String> inputFileNames, String outputFileName, boolean lastTask, boolean pipelining){
        id = taskID;
        this.command = commands;
        this.inputFileNames = inputFileNames;
        this.outputFileName = outputFileName;
        this.lastTask = lastTask;
        this.pipelining = pipelining;
    }

    public boolean parentIsDone(String pID){
        for(String p: parents){
            if(p.equals(pID)){
                Log.debug("Parent Task " + p + " finished.");
                parents.remove(p);
                if(parents.isEmpty()){
                    ready = true;
                }
                return true;
            }
        }
        return false;
    }
    public List<String> getParents(){ return parents;}

    public void addChild(String id){ children.add(id); }

    public boolean getPipelining(){ return pipelining; }

    public void setOutputIpAndPort(InetSocketAddress ipAndPort){ outputIpAndPort.add(ipAndPort); }
    public boolean hasOutputIpAndPort(){
        if(outputIpAndPort.isEmpty() && lastTask){
            try{
                outputIpAndPort.add(new InetSocketAddress("192.168.178.0", Config.basePort));    // returns loopback interface ip --> will be translated to "send back to manager"
                return true;
            }catch ( Exception e){
                Log.error("Couldn't set Manager as Output-Target.");
            }
        }
        Log.error("hasOutput:" + outputIpAndPort + " children: "+ children);
        return (outputIpAndPort.size() == children.size() && !outputIpAndPort.isEmpty());
    }
    public List<InetSocketAddress> getOutputIpAndPort(){ return outputIpAndPort;}

    public void setInProgress(){ inProgress = true; }
    public boolean inProgress(){ return inProgress; }

    public boolean isTask(){ return isTask; }

    public boolean isDone(){
        return done;
    }
    public void setDone() { done = true; }

    public void setFailed() { failed = true; }
    public boolean hasFailed(){return failed;}

    public int getInputServerPort(){ return inputServerPort; }
    public void setInputServerPort(int port){ inputServerPort = port; }

    public void setDependencies(List<String> parentList){
        parents = parentList;
    }
    public List<String> getDependencies(){
        return parents;
    }

    public List<String> getInputFileNames(){ return inputFileNames;}

    public List<String> getCommand(){
        return command;
    }
    public String getOutputFileName(){
        return outputFileName;
    }

    public void setWorker(WorkerHandle w){
        worker = w;
    }
    public WorkerHandle getWorker(){
        return worker;
    }

    public String getId(){return id; }
}
