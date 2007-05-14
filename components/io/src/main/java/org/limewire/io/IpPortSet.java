package org.limewire.io;



import java.util.Collection;
import java.util.TreeSet;


/**
 * Creates an <code>IpPort</code> collection.
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
