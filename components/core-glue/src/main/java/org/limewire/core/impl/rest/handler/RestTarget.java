package org.limewire.core.impl.rest.handler;

/**
 * REST API request targets.
 */
public enum RestTarget {
    HELLO("hello"), LIBRARY("library");
    
    private final String prefix;
    
    RestTarget(String target) {
        this.prefix = "/remote/" + target + "/";
    }
    
    public String pattern() {
        return prefix + "*";
    }
}
