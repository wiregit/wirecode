package de.kapsi.net.kademlia.io;

import java.net.SocketAddress;

import de.kapsi.net.kademlia.util.FixedSizeHashMap;

public class Filter {
    
    private FixedSizeHashMap counterMap;
    
    public Filter() {
        counterMap = new FixedSizeHashMap(64, 0.75f, true, 64);
    }
    
    public boolean allow(SocketAddress src) {
        DeltaCounter counter = (DeltaCounter)counterMap.get(src);
        if (counter == null) {
            counter = new DeltaCounter();
            counterMap.put(src, counter);
        }
        return counter.allow();
    }
    
    private static class DeltaCounter {
        
        private static final long DELTA_T = 250;
        
        private static final int MAX_MESSAGES = 10;
        
        private long contact = 0;
        private int count = 0;
        
        public boolean allow() {
            long time = System.currentTimeMillis();
            long delta = time - contact;
            
            contact = time;

            if (count < MAX_MESSAGES && delta < DELTA_T) {
                return (++count < MAX_MESSAGES);
            } else if (count >= MAX_MESSAGES
                    && delta < Math.max(DELTA_T * count/MAX_MESSAGES, 750L)) {
                count++;
                return false;
            } else {
                count = 0;
            }
            
            return true;
        }
    }
}
