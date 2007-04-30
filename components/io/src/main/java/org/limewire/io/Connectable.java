package org.limewire.io;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/** Exposes all information necessary for connecting to a host. */
public interface Connectable extends IpPort {
    
    /** Determines if the host is capable of receiving incoming TLS connections. */
    boolean isTLSCapable();
    
    /** An empty list, casted to an Connectable. */
    public static final List<Connectable> EMPTY_LIST = Collections.emptyList();
    /** An empty set, casted to an Connectable. */
    public static final Set<Connectable> EMPTY_SET = Collections.emptySet();
}
