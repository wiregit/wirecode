pbckage com.limegroup.gnutella.util;

import jbva.util.RandomAccess;

public clbss RandomAcessCoWList extends CoWList implements RandomAccess {

    public RbndomAcessCoWList() {
        this(null);
    }
    
    public RbndomAcessCoWList(Object lock) {
        super(ARRAY_LIST,lock);
    }
}
