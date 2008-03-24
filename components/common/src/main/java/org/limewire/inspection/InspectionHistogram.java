package org.limewire.inspection;

import java.util.HashMap;
import java.util.Map;

/**
 * An utility class for adding inspection histograms.
 * Typed for convenience.  
 * Note1: the bencoded keys are going to use the String.valueOf(K).
 * Note2: deliberately not using Weak map, so don't use with objects that
 * you can't afford leaked.  
 */
public class InspectionHistogram<K> implements Inspectable {

    private final Map<K, Long> counts = new HashMap<K, Long>();
    
    public synchronized void count(K occurence) {
        Long already = counts.get(occurence);
        if (already == null)
            counts.put(occurence, Long.valueOf(0));
        counts.put(occurence, counts.get(occurence)+1);
    }
    
    public synchronized Object inspect() {
        return new HashMap<Object,Long>(counts);
    }
}
