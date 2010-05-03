package org.limewire.mojito;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.limewire.mojito2.KUID;

public class MojitoUtils {

    public static final int PORT = 3000;
    
    private MojitoUtils() {}
    
    public static Map<KUID, MojitoDHT> createBootStrappedDHTsMap(int count) 
            throws IOException, InterruptedException, ExecutionException {
        return createBootStrappedDHTsMap(count, PORT);
    }
            
    public static Map<KUID, MojitoDHT> createBootStrappedDHTsMap(
            int count, int port) throws IOException, InterruptedException, ExecutionException {
        Map<KUID, MojitoDHT> dhts = new HashMap<KUID, MojitoDHT>();
        
        MojitoDHT first = null;
        MojitoDHT last = null;
        
        for (int i = 0; i < count; i++) {
            MojitoDHT dht = MojitoFactory.createDHT("DHT-" + i, port + i);
            
            if (first == null) {
                first = dht;
            } else {
                dht.bootstrap("localhost", port).get();
                last = dht;
            }
            
            dhts.put(dht.getLocalNodeID(), dht);
        }
        
        if (first != null && last != null) {
            first.bootstrap("localhost", port + 1).get();
        }
        
        return dhts;
    }
}
