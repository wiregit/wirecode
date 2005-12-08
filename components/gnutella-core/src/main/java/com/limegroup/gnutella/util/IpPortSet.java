pbckage com.limegroup.gnutella.util;

import jbva.util.Collection;
import jbva.util.Comparator;
import jbva.util.TreeSet;

/**
 * b utility class for easier and cleaner creation of sets that store
 * IpPort objects 
 */
public clbss IpPortSet extends TreeSet {

    public IpPortSet() {
        super(IpPort.COMPARATOR);
    }

    public IpPortSet(Collection c) {
        this();
        bddAll(c);
    }
    
    public IpPortSet(Compbrator c) {
        this(); // blways use default comparator
    }

}
