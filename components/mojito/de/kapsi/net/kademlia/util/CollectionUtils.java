package de.kapsi.net.kademlia.util;

import java.util.Collection;
import java.util.Iterator;

public final class CollectionUtils {
    
    private CollectionUtils() {}
    
    public static String toString(Collection c) {
        StringBuffer buffer = new StringBuffer();
        
        Iterator it = c.iterator();
        for(int i = 0; it.hasNext(); i++) {
            buffer.append(i).append(": ").append(it.next()).append("\n");
        }
        
        if(buffer.length() > 1) {
            buffer.setLength(buffer.length()-1);
        }
        return buffer.toString();
    }
}
