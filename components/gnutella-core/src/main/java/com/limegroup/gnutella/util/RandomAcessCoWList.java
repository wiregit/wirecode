package com.limegroup.gnutella.util;

import java.util.RandomAccess;

public class RandomAcessCoWList extends CoWList implements RandomAccess {

    public RandomAcessCoWList() {
        this(null);
    }
    
    public RandomAcessCoWList(Object lock) {
        super(ARRAY_LIST,lock);
    }
}
