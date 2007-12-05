package com.limegroup.gnutella.messages;

import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.IntervalSet;
import org.limewire.util.ByteOrder;


/**
 * Contains the logic for writing and reading IntervalSets
 * to and from a GGEP field.
 */
public class IntervalEncoder {
    
    public static void encode(long size, GGEP g, IntervalSet s) {
        List<Integer> bytes = new ArrayList<Integer>();
        List<Integer> shorts = new ArrayList<Integer>();
        List<Integer> b24 = new ArrayList<Integer>();
        List<Integer> ints = new ArrayList<Integer>();
        
        for (int i : s.encode(size)) {
            long l = i & 0xFFFFFFFF;
            if (l > 0xFFFFFFL)
                ints.add(i);
            else if (l > 0xFFFF)
                b24.add(i);
            else if (l > 0xFF)
                shorts.add(i);
            else
                bytes.add(i);
        }
        
        byte [] bytesB = new byte[bytes.size()];
        for (int i = 0; i < bytesB.length; i++)
            bytesB[i] = (byte)bytes.get(i).intValue();
        
        byte [] shortsB = new byte[shorts.size() * 2];
        for (int i = 0; i < shorts.size(); i++)
            ByteOrder.short2beb(shorts.get(i).shortValue(), shortsB, i * 2);
        
        byte [] b24B = new byte[b24.size() * 3];
        for (int i = 0; i < b24.size(); i++) {
            int value = b24.get(i);
            b24B[i] = (byte)((value & 0xFF0000) >> 16);
            b24B[i+1] = (byte)((value & 0xFF00) >> 8);
            b24B[i+2] = (byte)(value & 0xFF);
        }
        
        byte [] intsB = new byte[ints.size() * 4];
        for (int i = 0; i < ints.size(); i++) 
            ByteOrder.int2beb(ints.get(i).intValue(), intsB, i * 4);
        
        if (bytesB.length > 0)
            g.put(GGEP.GGEP_HEADER_PARTIAL_RESULT_PREFIX+1, bytesB);
        if (shortsB.length > 0)
            g.put(GGEP.GGEP_HEADER_PARTIAL_RESULT_PREFIX+2, shortsB);
        if (b24B.length > 0)
            g.put(GGEP.GGEP_HEADER_PARTIAL_RESULT_PREFIX+3, b24B);
        if (intsB.length > 0)
            g.put(GGEP.GGEP_HEADER_PARTIAL_RESULT_PREFIX+4, intsB);
    }
    
    public static IntervalSet decode(long size, GGEP ggep) throws BadGGEPPropertyException{
        IntervalSet ret = new IntervalSet();
        for (int i = 1; i <= 4; i++ ) {
            String key = GGEP.GGEP_HEADER_PARTIAL_RESULT_PREFIX+i;
            if (ggep.hasKey(key)) {
                byte [] b = ggep.get(key);
                if (b == null)
                    continue;

                // is data valid?
                if (b.length % i != 0)
                    return null;

                for (int j = 0; j < b.length; j+=i) {
                    int nodeId = 0;
                    for (int k = 0; k < i; k++) {
                        nodeId <<= 8;
                        nodeId |= (b[j + k] & 0xFF);
                    }

                    ret.decode(size, nodeId);
                }
            }
        }
        return ret;
    }
}
