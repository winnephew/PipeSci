package Helper;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;


public class TCPSocket implements AutoCloseable {

    private int headerLength = PacketBuilder.headerLength;
    private int fileNameLength = PacketBuilder.fileNameLength;
    private Socket socket;
    private InetSocketAddress partner;
    private InputStream in;
    private OutputStream out;

    /*************************************************************************************
     * Constructors
     *************************************************************************************/

    private static int portNr = Config.basePort;
    public TCPSocket(String serverAddress, int serverPort)
            throws UnknownHostException, IOException {
        this.socket = new Socket();

        while(!this.socket.isBound() && portNr < (Config.basePort+100) ) {
            try {
                InetAddress add = InetAddress.getByName("0.0.0.0");//socket.getLocalAddress();
            /*for (Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces(); e.hasMoreElements();) {
                boolean found = false;
                NetworkInterface interf = e.nextElement();
                for (Enumeration<InetAddress> i = interf.getInetAddresses(); i.hasMoreElements();) {
                    InetAddress addr = i.nextElement();
                    if(addr.getHostAddress().contains("141.20.21")) {
                        //add = addr;
                        Log.error("myAddress: " + addr);
                        found = true;
                        break;
                    }
                }
                if(found){break;}
            }*/

                //Log.debug("InetAddress local: " + add.getHostAddress());
                this.socket.bind(new InetSocketAddress(add, portNr));

                Log.debug("Binding socket after:" + this.socket.getLocalAddress() + this.socket.getLocalPort());

            } catch (Exception e) {
                Log.error("Socket bind failed:" + e + " with PortNr: " + portNr);
            }
            portNr++;
        }

        this.socket.connect(new InetSocketAddress(serverAddress, serverPort));

        this.socket.setSoTimeout(500);
        this.socket.setKeepAlive(true);
        this.partner = (InetSocketAddress) socket.getRemoteSocketAddress();
        initializeStreams();
    }

    public TCPSocket(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setSoTimeout(500);
        socket.setKeepAlive(true);
        this.partner = (InetSocketAddress) socket.getRemoteSocketAddress();
        initializeStreams();
    }


    /*************************************************************************************
     * Interface
     *************************************************************************************/

    public void sendMessage(String type, String message) throws IOException {
        Log.debug("Sending Message to " + partner + ": " + type + " - " + message);
        byte[] header = PacketBuilder.constructHeader(type, message);
        out.write(header, 0, header.length);
        out.flush();

        if (!message.equals("")) {
            byte[] messageBytes = message.getBytes("UTF-8");
            out.write(messageBytes, 0, messageBytes.length);
            out.flush();
        }
    }

    public void sendMessage(String type, List<String> lMessage) throws IOException {
        Log.debug("Sending Message to " + partner + ": " + type + " - " + lMessage);
        String message = "";
        boolean first = true;
        for (String s : lMessage) {
            if (first) {
                message += s;
                first = false;
                continue;
            }
            message += ';' + s;
        }

        byte[] header = PacketBuilder.constructHeader(type, message);
        out.write(header, 0, header.length);
        out.flush();

        if (!message.equals("")) {
            byte[] messageBytes = message.getBytes("UTF-8");
            out.write(messageBytes, 0, messageBytes.length);
            out.flush();
        }
    }

    public long sendFile(File f) throws IOException {
        Log.debug("Sending File to " + partner + ": " + f.getName());

        InputStream fileIn = new BufferedInputStream( new FileInputStream(f));
        // Sende Header
        byte[] header = PacketBuilder.constructHeader("File", f);
        out.write(header, 0, header.length);
        out.flush();

        // Sende FileName
        byte[] fileName = new byte[fileNameLength];
        byte[] nameBytes = f.getName().getBytes("UTF-8");
        System.arraycopy(nameBytes, 0, fileName, 0,nameBytes.length);
        out.write(fileName, 0, fileNameLength);
        out.flush();

        // Sende Payload
        long sum = 0;
        byte[] bytes = new byte[8192];
        int count;
        while ((count = fileIn.read(bytes)) > 0) {
            sum += count;
            out.write(bytes, 0, count);
            out.flush();
        }

        fileIn.close();
        return sum;
    }

    public void streamFileHeader(String outputName) throws IOException {
        Log.debug("streaming FileHeader");
        // Sende Header
        byte[] header = PacketBuilder.constructHeader("File Stream", "");
        out.write(header, 0, header.length);
        out.flush();

        // Sende FileName
        byte[] fileName = new byte[fileNameLength];
        byte[] nameBytes = outputName.getBytes("UTF-8");
        System.arraycopy(nameBytes, 0, fileName, 0,nameBytes.length);
        out.write(fileName, 0, fileNameLength);
        out.flush();
    }

    private AtomicBoolean firstStream = new AtomicBoolean(true);
    public boolean streamFileEnded = false;
    public long streamFile(InputStreamBuffer fromFifo, int readerNum, String outputName) throws IOException{
        //Log.debug("streaming File");

        byte[] bytes = new byte[Config.stdBuffSize];
        int count;
        long sum = 0;
        while ((count = fromFifo.read(bytes, readerNum)) > 0) {
            if(count == -1){ streamFileEnded = true; break; }
            sum += count;
            out.write(bytes, 0, count);
            out.flush();
            if(firstStream.compareAndSet(true,false)){
                PacketBuilder.taskDelay.stop(); // Timer - first bytes of productive result data send!
            }
        }
        return sum;
    }

    private BufferedOutputStream fout;
    public Packet receivePacket() throws IOException {
        Packet p = new Packet();
        String retStr = "";
        byte[] header = receiveBytes(headerLength);
        String type = PacketBuilder.getHeaderType(header);
        p.type = type;

        // Calc Payload Size
        byte[] messageLength = new byte[8];
        System.arraycopy(header, 1, messageLength, 0, Long.SIZE/8);
        long packetByteNum = PacketBuilder.bytesToLong(messageLength);


        // Receive Filename in Case of File (and Handle)
        if(type.equals("File")){
            p.message = setupFileTransfer();    // return FileName as Message
        }
        boolean runStream = false;
        if(type.equals("File Stream")){
            p.message = setupFileTransfer(); // returns the FileName Bytes
            runStream = true;
            packetByteNum = Long.MAX_VALUE;
        }

        // Get Payload
        int buffSize = (packetByteNum < Config.stdBuffSize) ? (int) packetByteNum : Config.stdBuffSize;
        byte[] bytes = new byte[buffSize];
        int sum = 0;
        int logSum = 0;
        int count = 1; // init has to be > 0

        while (count > 0 || runStream) {
            try{
                count = in.read(bytes);
            } catch(SocketTimeoutException e){
                count = 0;
            }

            if(count == -1) {
                break;
            }

            if( count > 0 ){
                sum += count;
                logSum += count;
                switch (type) {
                    case "File":
                        fout.write(bytes,0,count);
                        fout.flush();
                        break;
                    case "File Stream":
                        sum = 0;
                        fout.write(bytes,0,count);
                        fout.flush();
                        break;
                    default:
                        retStr += new String(bytes, "UTF-8");
                        break;
                }

                // make sure we take exactly the right amount of bytes
                long lastBatch = packetByteNum - sum;
                if (lastBatch < buffSize) {
                    bytes = new byte[(int) lastBatch];
                }
            }
        }

        switch (type) {
            case "Command":
            case "OutputTargets":
                List<String> retList = Arrays.asList(retStr.split(";"));
                p.command = retList;
                break;
            case "File":
            case "File Stream":
                fout.close();
                break;
            default:
                p.message = retStr;
                break;
        }

        if(!type.equals("")){
            Log.debug("Received Packet from " + partner + " - type: " + type + ", message: " + p.message + ", byteNum: " + logSum);
        }

        return p;
    }

    public ConnectionHandle getConnectionHandle() throws IOException {
        return new ConnectionHandle(socket);
    }

    public void close() throws IOException {
        out.close();
        in.close();
        socket.close();
    }


    /*************************************************************************************
     * Internal
     *************************************************************************************/

    private void initializeStreams() throws IOException {
        out = new BufferedOutputStream(socket.getOutputStream());
        in = new BufferedInputStream(socket.getInputStream());
    }

    public File inputDirectory;
    private String setupFileTransfer() throws IOException{
        byte[] fileNameBytes = receiveBytes(fileNameLength);
        String fileName = new String(fileNameBytes, "UTF-8").trim();
        try{
            if(inputDirectory == null){
                Log.error("Receiving File into Program directory: " + fileName + " from " + partner);
                inputDirectory = new File(System.getProperty("user.dir"));
            }
            fout = new BufferedOutputStream(new FileOutputStream(inputDirectory + "/" + fileName));
            Log.debug("Opened file outputstream to save file " + fileName + " from : " +partner );
        }
        catch(IOException e){
            Log.error("Couldn't open file output stream from " + partner + ".");
        }

        return fileName;
    }

    private byte[] receiveBytes(int ByteNum) throws IOException{
        byte[] bytes = new byte[ByteNum];
        int bcount;
        int bsum = 0;
        while ((bcount = in.read(bytes)) >= 0) {
            bsum += bcount;

            if (bsum == ByteNum) {
                break;
            } else if (ByteNum - bsum < ByteNum) {
                bytes = new byte[ByteNum - bsum];
            }
        }

        return bytes;
    }

}