package org.limewire.io;



import java.util.Collection;
import java.util.TreeSet;


/**
 * A utility class to easily construct a {@link TreeSet} with
 * {@link IpPort#COMPARATOR}.
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
