package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * a utility class for easier and cleaner creation of sets that store
 * IpPort objects 
 */
public class IpPortSet extends TreeSet {

    public IpPortSet() {
        super(IpPort.COMPARATOR);
    }

    public IpPortSet(Collection c) {
        this();
        addAll(c);
    }
    
    public IpPortSet(Comparator c) {
        this(); // always use default comparator
    }

}
