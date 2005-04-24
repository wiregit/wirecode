package com.limegroup.gnutella.io;


/**
 * Denotes the class can handle both reading and writing, doling out the
 * the reading/writing to specific delegate classes.
 */
public interface NIOMultiplexor extends ReadHandler, WriteHandler {
    
    /**
     * Retrieves the channel that can be used to read any data that was
     * temporarily stored by a ReadHandler.
     */
    public java.nio.channels.ReadableByteChannel getReadChannel();
    
    
    /** Retrieves the current ReadHandler */
    public ReadHandler getReadHandler();
    
    /** Retrieves the current WriteHandler */
    public WriteHandler getWriteHandler();
    
    /**
     * Sets the new ReadHandler.
     * If there is data left in an earlier read's buffer, it is immediately
     * written to the new ReadHandler if that handler implements the WritableByteChannel
     * interface.
     */
    public void setReadHandler(ReadHandler reader) throws java.io.IOException;
    
    /** 
     * Sets the new WriteHandler.
     * If there is data left in the earlier write's buffer, it is immediately
     * passed to the new WriteHandler if that handler implements the ReadableByteChannel
     * interface.
     */
    public void setWriteHandler(WriteHandler writer) throws java.io.IOException;
    
}