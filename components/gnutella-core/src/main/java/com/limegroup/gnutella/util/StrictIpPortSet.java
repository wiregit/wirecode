package com.limegroup.gnutella.util;

import java.util.Collection;
import java.util.TreeSet;

/**
 * A stricter genericized version of IpPortSet that lets you
 * have IpPortSets restricted to IpPort subclasses. 
 */
public class StrictIpPortSet<T extends IpPort> extends TreeSet<T> {

    public StrictIpPortSet() {
        super(IpPort.COMPARATOR);
    }

    public StrictIpPortSet(Collection<? extends T> c) {
        this();
        addAll(c);
    }
}