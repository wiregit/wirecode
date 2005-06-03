package com.limegroup.gnutella.util;

import java.util.Iterator;

/**
 *  A dual iterator that takes one element from the first collection, 
 *  then one from the second. 
 */
public class DualFlipIterator extends DualIterator {

    public DualFlipIterator(Iterator a, Iterator b) {
        super(a, b);
    }

    public Object next() {
        if (onOne) {
            if (i1.hasNext()) {
                onOne = false;
                return i1.next();
            } else 
                return i2.next();
        } else {
            if (i2.hasNext()) {
                onOne = true;
                return i2.next();
            } else
                return i1.next();
        }
    }
}
