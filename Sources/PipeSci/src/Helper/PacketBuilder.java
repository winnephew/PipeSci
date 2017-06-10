package Helper;

import java.io.File;


public class PacketBuilder {

    PacketBuilder(){    }

    public static int headerLength = 9;
    public static int fileNameLength = 255;
    public static Timer taskDelay = new Timer("placeholder");

    public static byte[] constructHeader(String type, Object message){

        // Header: 9 Bytes          || MessageType - 1 Byte || MessageLength (in Bytes) - 8 Byte ||

        // Note: in case of files another [fileNameLength] Bytes from the payload will be read as optional header, see: TCPSocket.setupFileTransfer()

        /*  Message Types:
            0:  Control
            1:  File
         */

        byte[] header = new byte[headerLength];

        // Set Message Type
        switch(type){
            case "Register":
                header[0] = 1;
                break;
            case "Command":
                // Sending the Command which is to be executed
                header[0] = 2;
                break;
            case "FileNames":
                header[0] = 3;
                break;
            case "Task Finished":
                header[0] = 4;
                break;
            case "Task Failed":
                header[0] = 5;
                break;
            case "Open Server":
                header[0] = 6;
                break;
            case "Output not Forwarded":
                header[0] = 7;
                break;
            case "Pipelining":
                header[0] = 8;
                break;
            case "OutputTargets":
                header[0] = 9;
                break;
            case "File Stream":
                header[0] = 125;
                break;
            case "File":
                header[0] = 126;
                break;
            case "Terminate":
                header[0] = 127;
                break;
            default:
                header[0] = 0;
                break;

        }

        // Set Message Length
        byte[] messageLength = new byte[8];
        long length = 0;

        if(message instanceof File){
            File f = (File) message;
            length = f.length();
        }

        if(message instanceof String){
            if(!message.equals("")){
                String s = (String) message;
                byte[] messageBytes = s.getBytes();
                length = messageBytes.length;
            }else{
                length = 0;
            }
        }

        messageLength = longToBytes( length );
        System.arraycopy(messageLength, 0, header, 1, Long.SIZE/8 );

        return header;
    }

    public static byte[] longToBytes(long l) {
        byte[] result = new byte[Long.SIZE/8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte)(l & 0xFF);
            l >>= Byte.SIZE;
        }
        return result;
    }

    public static long bytesToLong(byte[] b) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (b[i] & 0xFF);
        }
        return result;
    }

    public static String getHeaderType(byte[] header){
        String type = "";
        switch (header[0]) {
            case 0:
                //Log.info("Empty package");
                break;
            case 1:
                type = "Register";
                break;
            case 2:
                taskDelay = new Timer("TaskDelay"); // first MessageBytes that are being send to the Worker
                type = "Command";
                break;
            case 3:
                type = "FileNames";
                break;
            case 4:
                type = "Task Finished";
                break;
            case 5:
                type = "Task Failed";
                break;
            case 6:
                type = "Open Server";
                break;
            case 7:
                type = "Output not Forwarded";
                break;
            case 8:
                type = "Pipelining";
                break;
            case 9:
                type= "OutputTargets";
                break;
            case 126:
                type = "File";
                break;
            case 125:
                type = "File Stream";
                break;
            case 127:
                type = "Terminate";
                break;
            default:
                Log.error("Unknown package type - " + Log.localMachine + " Byte: " + header[0]);
                break;
        }
        return type;
    }
}
