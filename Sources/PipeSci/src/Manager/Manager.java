package Manager;

import java.io.*;
import java.net.ServerSocket;
import Helper.Log;
import Helper.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Manager {

    public static Scheduler scheduler;
    private boolean run = true;
    private ExecutorService executorService = Executors.newCachedThreadPool();

    public Manager(String xmlFilePath) throws InterruptedException, IOException {

        scheduler = new Scheduler(xmlFilePath);

        // Receive result File on another Port
        startResultServer();

        // Server to Manage and communicate with Workers
        startServer();
    }


    private void startServer() {

        try (ServerSocket serverSocket = new ServerSocket(Config.basePort)) {
            while (true) {
                // wait for connection then create streams
                Log.debug("Manager Server: Waiting for Connections");
                try {
                    TCPSocket tcpSocket = new TCPSocket(serverSocket.accept());
                    Log.debug("Manager Server: New Connection - " + tcpSocket.getConnectionHandle().toString());
                    new ServerSlave(tcpSocket);
                } catch (Exception e) {
                    Log.info(e);
                }
            }
        } catch (Exception e) {
            Log.error("Manager Server: Error when creating/using the ServerSocket");
        }

    }

    private void startResultServer(){
        executorService.submit( new Runnable() {

            @Override
            public void run() {
                try (ServerSocket serverSocket = new ServerSocket( (Config.basePort+1) )) {
                    while (true) {
                        Log.debug("Manager - Server Socket Result-Server geöffnet: Warten auf Verbindungsaufbau");
                        try {
                            TCPSocket s = new TCPSocket(serverSocket.accept());
                            Log.debug("Manager Result-Server: New Connection");
                            executorService.submit( new ManagerReceiverSlave(s) );
                        } catch (Exception e) {
                            Log.error("Manager Result-Server: Error when accepting Connection: " + e);
                        }
                    }

                } catch (Exception e) {
                    Log.error("Manager Result-Server: Error when creating ServerSockets");
                }
            }
        });
    }




}
