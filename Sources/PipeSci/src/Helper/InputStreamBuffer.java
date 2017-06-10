package Helper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class InputStreamBuffer {

    private InputStream source;
    private byte[] buffer = new byte[Config.stdBuffSize * 40];
    private int writePosition = 0;
    private List<Integer> readPositions = new ArrayList<Integer>();

    private boolean shutdown = false;

    private AtomicBoolean lock = new AtomicBoolean(false);
    private AtomicInteger currentReads = new AtomicInteger(0);

    public InputStreamBuffer(InputStream in, int readers) {
        source = in;
        for(int i = 0; i < readers; i++){
            readPositions.add(0);
        }
        Bufferer bufferer = new Bufferer();
        Thread t = new Thread(bufferer);
        t.start();
    }

    public int read(byte[] bytes, int readerId){
        //Log.debug("reading Fifo");

        currentReads.getAndIncrement();
        while(true){
            if( !lock.get()){
                int startPosition = readPositions.get(readerId);
                int volume = (startPosition + Config.stdBuffSize < writePosition) ? Config.stdBuffSize : (writePosition - startPosition);
                if (volume == 0 && shutdown) {
                    return -1;
                }

                try {

                    System.arraycopy(buffer, startPosition, bytes, 0, volume);
                    readPositions.set(readerId, (startPosition + volume));

                    currentReads.getAndDecrement();

                    return volume;
                }
                catch (Exception e){
                    Log.error("inputstreambuffer read error:" + e + " " + startPosition + volume + buffer.length + bytes.length);
                }
            }else{
                currentReads.getAndDecrement();
                try{
                    Thread.sleep(500);
                }catch(Exception e){
                    Log.error("InputStreamBuffer read: " + e);
                }
            }
        }
    }

    private class Bufferer implements Runnable{

        private void prepareBuffer(){
            if(lock.compareAndSet(false,true)){
                try{
                    while( currentReads.get() > 0 ){
                        // wait till current Reads have finished
                        Thread.sleep(100);
                    }

                    int min = Collections.min(readPositions);
                    byte[] newBuffer = new byte[buffer.length * 2];
                    System.arraycopy(buffer, min, newBuffer, 0, buffer.length-min);
                    buffer = newBuffer;
                    for (int i = 0; i < readPositions.size(); i++){
                        readPositions.set(i, readPositions.get(i) - min);
                    }
                    writePosition -= min;
                }
                catch (Exception e){
                    Log.error("InputStreamBuffer prepare: " + e);
                }
                finally{
                    lock.set(false);
                }
            };
        }

        @Override
        public void run(){
            byte[] temp = new byte[Config.stdBuffSize];
            int count;
            while(true){
                try {
                    count = source.read(temp);
                    if(count == -1){
                        break;
                    }

                    if(writePosition + count >= buffer.length) {
                        prepareBuffer();
                    }
                    System.arraycopy(temp, 0, buffer, writePosition, count);
                    writePosition += count;
                }
                catch(Exception e){
                    Log.error("InputStreamBuffer run: " + e);
                }
            }
            shutdown = true;
        }
    }
}
