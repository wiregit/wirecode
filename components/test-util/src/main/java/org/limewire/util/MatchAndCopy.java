package org.limewire.util;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * A Hamcrest IsInstanceOf style matcher that stores the last match in
 *  a retrievable slot. 
 */
public class MatchAndCopy<X> extends BaseMatcher<X> {

    private X lastMatch = null;
    private Class<?> theClass;
    
    public MatchAndCopy(Class<?> theClass) {
        this.theClass = theClass;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(Object item) {
        if (theClass.isInstance(item)) {
            lastMatch = (X) item;
            return true;
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
    }
    
    public X getLastMatch() {
        return lastMatch;
    }
}