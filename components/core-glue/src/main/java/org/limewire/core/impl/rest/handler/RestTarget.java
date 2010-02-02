package org.limewire.core.impl.rest.handler;

/**
 * REST API request targets.
 */
public enum RestTarget {
    HELLO("hello"), LIBRARY("library"), SEARCH("search");
    
    private final String pattern;
    
    RestTarget(String pattern) {
        this.pattern = pattern;
    }
    
    public String pattern() {
        return pattern;
    }
}
