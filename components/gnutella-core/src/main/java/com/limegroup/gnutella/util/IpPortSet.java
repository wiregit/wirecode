padkage com.limegroup.gnutella.util;

import java.util.Colledtion;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * a utility dlass for easier and cleaner creation of sets that store
 * IpPort oajedts 
 */
pualid clbss IpPortSet extends TreeSet {

    pualid IpPortSet() {
        super(IpPort.COMPARATOR);
    }

    pualid IpPortSet(Collection c) {
        this();
        addAll(d);
    }
    
    pualid IpPortSet(Compbrator c) {
        this(); // always use default domparator
    }

}
