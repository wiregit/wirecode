package com.limegroup.gnutella.util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.limewire.io.IpPort;
import org.limewire.io.NetworkUtils;

/**
 * Utility class keeps track of class C networks and an associated count.
 */
public class ClassCNetworks {
    private Map<Integer, Integer> counts = new HashMap<Integer,Integer>();
    
    /** Utility comparator to use for sorting class C networks */
    static final Comparator<Map.Entry<Integer,Integer>> CLASS_C_COMPARATOR =
        new Comparator<Map.Entry<Integer, Integer>>() {
        public int compare(Map.Entry<Integer, Integer> a, Map.Entry<Integer, Integer> b) {
            return b.getValue().compareTo(a.getValue());
        }
    };
    
    public void addAll(Collection<? extends IpPort> c) {
        for (IpPort ip : c)
            add(ip.getInetAddress(), 1);
    }
    
    public void add(InetAddress addr, int count) {
        add(NetworkUtils.getClassC(addr), count);
    }
    
    public void add(int masked, int count) {
        Integer num = counts.get(masked);
        if (num == null) {
            num = Integer.valueOf(0);
        }
        num = Integer.valueOf(num.intValue() + count);
        counts.put(masked, num);
    }
    
    public List<Map.Entry<Integer, Integer>> getTop() {
        List<Map.Entry<Integer, Integer>> ret = 
            new ArrayList<Map.Entry<Integer,Integer>>(counts.size());
        ret.addAll(counts.entrySet());
        Collections.sort(ret, CLASS_C_COMPARATOR);
        return ret;
    }
    
    /** returns the top n class C networks in easy to bencode format */ 
    public List<Integer> getTopInspectable(int number) {
        List<Map.Entry<Integer, Integer>> top = getTop();
        List<Integer> ret = new ArrayList<Integer>(number * 2);
        for (Map.Entry<Integer, Integer> entry : top) {
            if (ret.size() >= 2 * number)
                break;
            ret.add(entry.getKey());
            ret.add(entry.getValue());
        }
        return ret;
    }
}