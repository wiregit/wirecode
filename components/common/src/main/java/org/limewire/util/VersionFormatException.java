package org.limewire.util;

/**
 * Thrown upon a version parsing error when the provided version format 
 * is ill-formed.
 * 
 */
public class VersionFormatException extends Exception {
    
    VersionFormatException() {
        super();
    }
    
    VersionFormatException(String s) {
        super(s);
    }
}