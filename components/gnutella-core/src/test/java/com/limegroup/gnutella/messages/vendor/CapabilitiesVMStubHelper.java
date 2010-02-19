package com.limegroup.gnutella.messages.vendor;

import java.util.Map;
import java.util.TreeMap;

import org.limewire.collection.Comparators;

public class CapabilitiesVMStubHelper {
    
    public static CapabilitiesVM makeCapabilitiesWithSimpp(int simppNumber) {
        Map<byte[], Integer> caps = new TreeMap<byte[], Integer>(new Comparators.ByteArrayComparator());
        caps.put(new byte[] { 'S', 'I', 'P', 'M' }, simppNumber);
        return new CapabilitiesVMImpl(caps);
    }
    
    public static CapabilitiesVM makeCapabilitiesWithSimpp(int version, int newVersion, int keyVersion) {
        Map<byte[], Integer> caps = new TreeMap<byte[], Integer>(new Comparators.ByteArrayComparator());
        caps.put(new byte[] { 'S', 'I', 'P', 'M' }, version);
        caps.put(new byte[] { 'S', 'M', 'P', 'V' }, newVersion);
        caps.put(new byte[] { 'S', 'M', 'P', 'K' }, keyVersion);
        return new CapabilitiesVMImpl(caps);
    }
    
    public static CapabilitiesVM makeCapabilitiesWithUpdate(int id) {
        Map<byte[], Integer> caps = new TreeMap<byte[], Integer>(new Comparators.ByteArrayComparator());
        caps.put(new byte[] { 'U', 'P', 'L', 'M' }, id);
        return new CapabilitiesVMImpl(caps);        
    }
}
