package Helper;

import java.net.InetSocketAddress;
import java.net.Socket;


public class ConnectionHandle {

    public InetSocketAddress clientSocketAddress;
    public InetSocketAddress serverSocketAddress;

    public ConnectionHandle(Socket s){
        clientSocketAddress = (InetSocketAddress) s.getRemoteSocketAddress();
        serverSocketAddress = (InetSocketAddress) s.getLocalSocketAddress();
    }

    public String toString() {
        return "Client: " + clientSocketAddress.getAddress() + ":" + clientSocketAddress.getPort() + ", Server: " + serverSocketAddress.getAddress() + ":" + serverSocketAddress.getPort();
    }
}
