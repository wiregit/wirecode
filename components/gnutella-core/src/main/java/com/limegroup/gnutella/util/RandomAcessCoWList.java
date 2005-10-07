package com.limegroup.gnutella.util;

import java.util.RandomAccess;

public class RandomAcessCoWList extends CoWList implements RandomAccess {

    public RandomAcessCoWList() {
        super(ARRAY_LIST);
    }
}
