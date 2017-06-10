package com.Pipeformance;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;

public class FileAccess {

    public static String readNum(Reader in) throws IOException {
        String num = "";
        char c;
        while((c = (char)in.read()) != ' '){
            num += c;
        }
        return num;
    }

    public static void writeNum(Writer out, String i) throws IOException{
        out.write(i);
        out.flush();
    }

    //public static long c = 0;
    public static boolean readPart(Reader in, ArrayList<String> part) throws  IOException{
        boolean notEOF = true;
        int partCount = 0;
        while( partCount < Main.partUnits ){
            String numStr = FileAccess.readNum(in);
            if(numStr.equals("EOF")){
                notEOF = false;
                //Log.debug("Read bytes: " + c);
                break;
            };
            part.add(numStr);
            partCount ++;
            //c+= numStr.getBytes().length;
        }
        return notEOF;
    }

    public static long[] writePart(Writer out, ArrayList<String> outPart) throws IOException{
        long[] count = new long[2];
        long bytes = 0;

        for(String data: outPart){
            String message = data + " ";
            bytes += message.getBytes().length;
            out.write(message);
        }
        out.flush();

        count[0] = outPart.size();
        count[1] = bytes;
        return count;
    }

}
