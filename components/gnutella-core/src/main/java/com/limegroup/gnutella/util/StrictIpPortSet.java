package com.limegroup.gnutella.util;


import java.util.Arrays;
import java.util.Collection;
import java.util.TreeSet;

import org.limewire.io.IpPort;


/**
 * A stricter genericized version of IpPortSet that lets you
 * have IpPortSets restricted to IpPort subclasses. 
 */
public class StrictIpPortSet<T extends IpPort> extends TreeSet<T> {

    /** Constructs an empty set. */
    public StrictIpPortSet() {
        super(IpPort.COMPARATOR);
    }

    /** Constructs a set with the given initial IpPorts. */
    public StrictIpPortSet(Collection<? extends T> c) {
        this();
        addAll(c);
    }
    
    /** Constructs a set with the given initial IpPorts. */
    public StrictIpPortSet(T... ipps) {
        this(Arrays.asList(ipps));
    }
}