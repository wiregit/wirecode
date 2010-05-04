package org.limewire.mojito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.limewire.mojito2.KUID;
import org.limewire.mojito2.settings.KademliaSettings;

public class MojitoUtils {

    public static final int PORT = 3000;
    
    private MojitoUtils() {}
    
    public static Map<KUID, MojitoDHT> createBootStrappedDHTsMap(int factor) 
            throws IOException, InterruptedException, ExecutionException {
        return createBootStrappedDHTsMap(factor, PORT);
    }
            
    public static Map<KUID, MojitoDHT> createBootStrappedDHTsMap(
            int factor, int port) throws IOException, InterruptedException, ExecutionException {
        
        if (factor < 1) {
            throw new IllegalArgumentException("factor=" + factor);
        }
        
        int k = KademliaSettings.REPLICATION_PARAMETER.getValue();
        int count = k * factor;
        
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
    
    public static List<MojitoDHT> createBootStrappedDHTs(int factor) 
            throws IOException, InterruptedException, ExecutionException {
        return createBootStrappedDHTs(factor, PORT);
    }
    
    public static List<MojitoDHT> createBootStrappedDHTs(int factor, int port) 
            throws IOException, InterruptedException, ExecutionException {
        Collection<MojitoDHT> dhts = createBootStrappedDHTsMap(factor, port).values();
        return new ArrayList<MojitoDHT>(dhts);
    }
}
