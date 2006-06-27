package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.TreeSet;

/**
 * a utility class for easier and cleaner creation of sets that store
 * IpPort objects 
 */
public class IpPortSet extends TreeSet<IpPort> {

    public IpPortSet() {
        super(IpPort.COMPARATOR);
    }

    public IpPortSet(Collection<? extends IpPort> c) {
        this();
        addAll(c);
    }
    
}
