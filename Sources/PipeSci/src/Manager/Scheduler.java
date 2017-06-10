package Manager;


import Helper.Config;
import Helper.ConnectionHandle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import Helper.Log;
import Helper.Timer;

public class Scheduler extends Thread{


    public static Map<String, Task> jobMap = new ConcurrentHashMap<String, Task>();   // jobMap: XML-JobID, TaskObject
    public static Map<ConnectionHandle, WorkerHandle> workerMap = new ConcurrentHashMap<ConnectionHandle, WorkerHandle>();
    private Timer timer;

    /*************************************************************************************
                                            Constructor
     *************************************************************************************/

    Scheduler(String xmlFilePath){
        // Fills jobMap
        getTasksFromXML(xmlFilePath);

        // Sets Task dependencies of Jobs in jobMap
        for (Task t : jobMap.values()) {

            t.ready = true;
            List<String> parents = new ArrayList<String>(t.getDependencies());

            if( !parents.isEmpty() ){
                for (String parent : parents) {
                    if(jobMap.containsKey(parent)){
                        Task p = jobMap.get(parent);
                        if (!p.isDone()) {
                            t.ready = false;
                            p.addChild(t.getId());
                        }
                    }
                }
            }
        }
    }

    /*************************************************************************************
                                        Interface
     *************************************************************************************/

    public synchronized Task scheduleWorker(ConnectionHandle connectionHandle, WorkerHandle w){
        Task emptyTask = new Task();

        for (Task t : jobMap.values()) {
            if(t.isDone() || t.hasFailed()){
                continue;
            }
            if( !t.inProgress() ){  // t.ready &&
                t.setInProgress();

                List<String> parents = t.getParents();
                if(!parents.isEmpty()){

                    t.setInputServerPort( getPort(connectionHandle.clientSocketAddress.getAddress()) );

                    for (String parent: parents){
                        Task p = jobMap.get(parent);
                        p.setOutputIpAndPort(new InetSocketAddress(connectionHandle.clientSocketAddress.getAddress(), t.getInputServerPort() ));
                        jobMap.put(parent, p);
                    }
                }

                w.setTask(t);
                workerMap.put(connectionHandle, w);
                Log.debug("Scheduling Worker - Task: " + t.getId());
                return t;
            }
        }
        //Log.debug("Scheduling Worker - No Task left to schedule.");
        return emptyTask;
    }

    public boolean workerWait(ConnectionHandle connectionHandle, WorkerHandle w){
        workerMap.put(connectionHandle, w);

        if(workerMap.size() < Config.waitWorkers){
            Log.debug("Got " + workerMap.size() + " workers, waiting for: " + Config.waitWorkers);
            return true;
        }else{
            timer = new Timer("Makespan");
            return false; // Start Execution
        }
    }

    public boolean signalTaskDone(ConnectionHandle connectionHandle){
        if(!workerMap.containsKey(connectionHandle)){
            Log.error("Scheduler Error: Worker unknown.");
        }
        WorkerHandle w = workerMap.get(connectionHandle);
        Task t = w.getTask();
        t.setDone();
        resolveDependencies(t.getId());
        jobMap.put(t.getId(), t);

        boolean workflowDone = true;
        for(Task task : jobMap.values()){
            if(!task.isDone()){
                workflowDone = false;
            }
        }
        if(workflowDone){
            timer.stop();
            return true;
        }
        return false;
    }


    public void signalTaskFailed(ConnectionHandle connectionHandle){
        if(!workerMap.containsKey(connectionHandle)){
            Log.error("Scheduler Error: Worker unknown.");
        }
        WorkerHandle w = workerMap.get(connectionHandle);
        Task t = w.getTask();
        t.setFailed();
        jobMap.put(t.getId(), t);
    }

    public Task refreshTask(String id){
        return jobMap.get(id);
    }

    /*************************************************************************************
                                         Internal
     *************************************************************************************/

    int portCount = Config.basePort+1;
    private int getPort(InetAddress ip){
        for(ConnectionHandle connection : workerMap.keySet()){
            if (connection.clientSocketAddress.getAddress() == ip){
                portCount += 1;
            }
        }
        return portCount;
    }

    private void resolveDependencies(String taskID){
        for(Task t: jobMap.values()){
            t.parentIsDone(taskID);
        }
    }

    private void getTasksFromXML(String xmlFilePath) {
        try {

            File xmlFile = new File(System.getProperty("user.dir") + "/" + xmlFilePath);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // analyze jobs
            NodeList jobNodeList = doc.getElementsByTagName("job");

            for (int i = 0; i < jobNodeList.getLength(); i++) {
                Node jobNode = jobNodeList.item(i);

                if (jobNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element job = (Element) jobNode;

                    String id = job.getAttribute("id");
                    String name = job.getAttribute("name");

                    List<String> command = getCommand(job, name);

                    boolean pipelining = getPipelining(job, id);

                    String output = "";

                    // get Input Files
                    List<String> fileNames = new ArrayList<String>();
                    boolean lastFile = false;
                    NodeList usesFiles = job.getElementsByTagName("uses");
                    for (int j = 0; j < usesFiles.getLength(); j++) {
                        Node usesFile = usesFiles.item(j);
                        Element file = (Element) usesFile;
                        if(file.getAttribute("link").equals("input")){
                            fileNames.add(file.getAttribute("name"));
                        }
                        if(file.getAttribute("link").equals("output")){
                            output = file.getAttribute("name");
                        }
                        if(file.hasAttribute("transfer") && file.getAttribute("transfer").equals("true")){
                            lastFile = true;
                        }

                    }

                    // Build Task
                    Task t = new Task(id, name, command, fileNames, output, lastFile, pipelining);

                    jobMap.put(id, t);
                    Log.debug("Job added: " + id + " \n" + command );
                }
            }

            // Find parent job dependencies
            setDependencies(doc);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDependencies(Document doc) {
        // analyze dependencies
        NodeList dependencyNodeList = doc.getElementsByTagName("child");

        for (int i = 0; i < dependencyNodeList.getLength(); i++) {
            Node depNode = dependencyNodeList.item(i);

            if (depNode.getNodeType() == Node.ELEMENT_NODE) {
                Element dependency = (Element) depNode;
                String id = dependency.getAttribute("ref");

                List<String> parentList = new ArrayList<String>();
                NodeList parentNodeList = dependency.getElementsByTagName("parent");
                for (int j = 0; j < parentNodeList.getLength(); j++) {
                    Node parentNode = parentNodeList.item(j);
                    Element parent = (Element) parentNode;
                    parentList.add(parent.getAttribute("ref"));
                }

                Log.debug("Dependencies: " + id + "  " + parentList);

                Task job = jobMap.get(id);
                job.setDependencies(parentList);
            }

        }
    }

    private boolean getPipelining(Element job, String id){
        if(Config.pipeliningSource.equals("workflow")){
            String pipelining = job.getAttribute("pipelining");
            if(pipelining.equals("true")) {
                return true;
            }
            else if(pipelining.equals("false")) {
                return false;
            }
            else {
                Log.error("Scheduler: pipeliningSource specified as 'workflow' but unknown attribute in the workflow XML: ID " + id + "Pipelining-Attribute" + pipelining);
            }
        }

        return Config.pipelining;
    }

    private List<String> getCommand(Element job, String program) {
        //  ./mImgtbl "rawdir/" "images-rawdir.tbl"

        List<String> command = new ArrayList<String>();

        switch (program) {
            case "ls":
            case "cat":
            case "java":
                command.add(program);
                break;
            default:
                command.add("./" + program);
                break;
        }

        NodeList arguments = job.getElementsByTagName("argument");

        for (int i = 0; i < arguments.getLength(); i++) {

            Node argument = arguments.item(i);

            // split arguments up into different parameters, when they contain whitespace --> might cause problems with some paths?
            String[] args = argument.getTextContent().split(" ");
            for (int j = 0; j < args.length; j++) {
                if (!args[j].equals("")) {
                    command.add(args[j]);
                }
            }

            // Get file arguments
            Element arg = (Element) argument;
            NodeList fileargs = arg.getElementsByTagName("file");
            for (int k = 0; k < fileargs.getLength(); k++) {
                Element filea = (Element) fileargs.item(k);
                command.add(filea.getAttribute("name"));
            }
        }
        return command;
    }
}
