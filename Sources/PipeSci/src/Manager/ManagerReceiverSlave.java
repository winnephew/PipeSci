package Manager;

import Helper.Log;
import Helper.Packet;
import Helper.TCPSocket;

public class ManagerReceiverSlave implements Runnable {

    private TCPSocket s;

    public ManagerReceiverSlave(TCPSocket s){
        this.s = s;
    }

    @Override
    public void run(){
        boolean run = true;
        while (run) {
            try {
                Packet packet = s.receivePacket();
                switch (packet.type) {
                    case "File":
                        Log.debug("Manager Result-Server: Receiving File");
                        break;
                    case "File Stream":
                        Log.debug("Manager Result-Server: Receiving File Stream");
                        break;
                    case "Terminate":
                        run = false;
                        break;
                    default:
                        //Log.error("Server Port 1251 - Unknown Packet: " + packet.type + " " + packet.message);
                        Thread.sleep(100);
                        break;
                }
            } catch (Exception e) {
                // No packet received
            }
        }
        try { s.close(); }
        catch (Exception e){
            Log.error("Could not close socket" +e );
        }
    }
}
