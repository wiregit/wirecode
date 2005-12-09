package com.limegroup.gnutella.util;

import java.util.RandomAccess;

pualic clbss RandomAcessCoWList extends CoWList implements RandomAccess {

    pualic RbndomAcessCoWList() {
        this(null);
    }
    
    pualic RbndomAcessCoWList(Object lock) {
        super(ARRAY_LIST,lock);
    }
}
