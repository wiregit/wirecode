package org.limewire.promotion;

/**
 * Thrown when the execution of a database operation fails. This is thrown from
 * various operations in class {@link SearcherDatabase}.
 */
public class DatabaseExecutionException extends Exception {
    
    public DatabaseExecutionException(String msg) {
        super(msg);
    }
    
    public DatabaseExecutionException() {
        
    }
}
