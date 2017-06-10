
import Manager.Manager;
import Worker.Worker;
import Helper.Log;
import Helper.Config;
import java.io.*;

public class Main{

    /*
        Arguments: manager || worker and ip of manager
     */
    public static void main(String[] args) throws InterruptedException, IOException {

        boolean worker = false;
        String masterIpAddress = "";
        boolean mIp = false;

        boolean master = false;
        String workflowFilePath = "";
        boolean xmlFilePath = false;


        for(String s: args) {
            if(mIp){
                masterIpAddress = s;
                mIp = false;
                continue;
            }
            if(xmlFilePath){
                workflowFilePath = s;
                xmlFilePath = false;
                continue;
            }
            switch(s){
                case "manager":
                    master = true;
                    break;
                case "worker":
                    worker = true;
                    break;
                case "--ip":
                    mIp = true;
                    break;
                case "--xml":
                    xmlFilePath = true;
                    break;
                default:
                    System.out.println("Argument invalid: " + s);
            }
        }

        if(worker && !masterIpAddress.equals("") ){
            Log log = new Log("Worker");
            Config conf = new Config();
            Worker w = new Worker(masterIpAddress);
        }else if(worker && masterIpAddress.equals("") ){
            System.out.println( "Ip-Address of Manager needed to initiate Worker. Please use '--ip [IP-Address]'.");
        }

        if(master && !workflowFilePath.equals("")){
            Log log = new Log("Manager");
            Config conf = new Config();
            Manager manager = new Manager(workflowFilePath);
        }else if(master && workflowFilePath.equals("")){
            System.out.println( "You have to pass the path of the XML-File of your workflow when initiating the master. Plese use '--xml [File-Path]'.");
        }
    }

}
