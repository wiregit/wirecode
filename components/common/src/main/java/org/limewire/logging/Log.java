package org.limewire.logging;

/**
 * Extends {@link org.apache.commons.logging.Log} and defines convenience methods 
 * to allow conditional message formatting. 
 */
public interface Log extends org.apache.commons.logging.Log {

    /**
     * Logs <code>message</code> as debug message and formats message
     * with <code>args</code> if {@link #isDebugEnabled()} is true.
     */
    void debug(String message, Object...args);
    
    void debug(String message, Object args);
    
    void debug(String message, Object arg1, Object arg2);
    
    void debug(String message, Object arg1, Object arg2, Object arg3);
    /**
     * Logs <code>message</code> as trace message and formats message
     * with <code>args</code> if {@link #isTraceEnabled()} is true.
     */
    void trace(String message, Object...args);

    void trace(String message, Object args);
    
    void trace(String message, Object arg1, Object arg2);
    
    void trace(String message, Object arg1, Object arg2, Object arg3);
    /**
     * Logs <code>message</code> as info message and formats message
     * with <code>args</code> if {@link #isInfoEnabled()} is true.
     */
    void info(String message, Object...args);
    
    void info(String message, Object args);
    
    void info(String message, Object arg1, Object arg2);
    
    void info(String message, Object arg1, Object arg2, Object arg3);
    /**
     * Logs <code>message</code> as warn message and formats message
     * with <code>args</code> if {@link #isWarnEnabled()} is true.
     */
    void warn(String message, Object...args);
    
    void warn(String message, Object args);
    
    void warn(String message, Object arg1, Object arg2);
    
    void warn(String message, Object arg1, Object arg2, Object arg3);
    
}
