package Helper;

import Manager.*;

import java.net.InetAddress;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ServerSlave extends Thread
{
    private TCPSocket socket;
    private ConnectionHandle connectionHandle;
    private boolean run = true;
    boolean allDone = false;

    public ServerSlave(TCPSocket socket)
    {
        this.socket = socket;
        this.start();
    }

    public void run()
    {
        try(TCPSocket s = socket)
        {
            WorkerHandle worker = new WorkerHandle();
            connectionHandle = s.getConnectionHandle();
            while(run)
            {
                try {
                    Packet packet = s.receivePacket();
                    // execute client requests
                    switch (packet.type) {
                        case "Register":
                            while(Manager.scheduler.workerWait(connectionHandle, worker)){
                                Thread.sleep(1000);
                            }
                            schedule(s, worker);
                            break;
                        case "Task Finished":
                            allDone = Manager.scheduler.signalTaskDone(connectionHandle);
                            schedule(s, worker);
                            break;
                        case "Task Failed":
                            Manager.scheduler.signalTaskFailed(connectionHandle);
                            schedule(s, worker);
                            break;
                        case "Output not Forwarded":

                            break;
                        case "File":
                            Log.debug("" + connectionHandle +" - File received: " + packet.message);
                            break;

                        default:
                            //Log.error("Unknown Packet: " + packet);
                            break;
                    }
                }
                catch(Exception e){

                }
            }
            socket.close();
            if(allDone){ System.exit(0); }
        }
        catch(Exception e)
        {
            Log.error("ServerSlave - Error: " + e);
        }
        Log.debug("ServerSlave - Connection Closed");
    }

    private void schedule(TCPSocket s, WorkerHandle worker) throws Exception{
        Task t = Manager.scheduler.scheduleWorker(connectionHandle, worker);

        if (!t.isTask()) {
            s.sendMessage("Terminate", "");
            run = false;
            return;
        }

        while(!Manager.scheduler.refreshTask(t.getId()).hasOutputIpAndPort()){
            Thread.sleep(100);          // wait until we have an outputIP (we're gonna schedule everything right now)
        }

        s.sendMessage("Pipelining", String.valueOf(t.getPipelining()));

        s.sendMessage("Command", t.getCommand());

        List<InetSocketAddress> outIpAndPort = t.getOutputIpAndPort();
        List<String> outputTargets = new ArrayList<>();
        for(InetSocketAddress out: outIpAndPort){
            if(out.getAddress().equals(InetAddress.getByName("192.168.178.0"))){
                outputTargets.add("manager:" + Config.basePort);
            }else{
                outputTargets.add(out.getAddress().toString().substring(1) + ":" + String.valueOf(out.getPort())); // cut off the "/"
            }
        }
        s.sendMessage("OutputTargets", outputTargets);

        List<String> inputFileNames = t.getInputFileNames();

        String files = "" + inputFileNames.size();
        for(String fileName: inputFileNames){
            files += ";" + fileName;
        }
        files += ";" + t.getOutputFileName();
        s.sendMessage("FileNames", files);

        boolean once = true;
        for(String fileName: inputFileNames){
            File f = new File( System.getProperty("user.dir") + "/" + fileName);
            if(f.exists() && !f.isDirectory()) {
                Log.debug("Sending input file from manager: " + fileName);
                s.sendFile(f);
            }else{
                Log.debug("Input file not available on manager: tell Worker to Open P2P Server Socket");
                if(once) {
                    s.sendMessage("Open Server", String.valueOf(t.getInputServerPort()));
                    once = false;
                }
            }
        }

    }
}