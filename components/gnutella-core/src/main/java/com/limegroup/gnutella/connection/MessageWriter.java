package com.limegroup.gnutella.connection;

import java.io.IOException;

import com.limegroup.gnutella.messages.Message;

/**
 * This interface abstracts out the ability to write data to the
 * network, removing this functionality from the connection classes,
 * allowing them to be almost completely agnostic to the underlying
 * transport layer -- particularly regarding the use of blocking
 * vs. non-blocking IO.
 */
public interface MessageWriter {

    /**
     * Writes pending messages to the network.
     * 
     * @return <tt>true</tt> if the message was successfully written, otherwise
     *  <tt>false</tt>
     * @throws IOException if there was an IO error writing the message
     */
    boolean write() throws IOException;
   
    /**
     * Writes the specified <tt>Message</tt> to the network.
     * 
     * @param m the <tt>Message</tt> to write 
     * @return <tt>true</tt> if the message was successfully written, otherwise
     *  <tt>false</tt>
     * @throws IOException if there was an IO error writing the message
     */
    boolean write(Message m) throws IOException;
    
    /**
     * Writes this message directly to the network without any buffering.
     * 
     * @param msg the <tt>Message</tt> to write
     */
    void simpleWrite(Message msg) throws IOException;
    
    /**
     * Flushes any data in the output stream.
     * 
     * @throws IOException if the connection is closed, or any other IO error
     *  occurs
     */
    void flush() throws IOException;
    
    /**
     * Returns whether or not there are pending, unsent messages for this 
     * writer.
     * 
     * @return <tt>true</tt> if there are pending messages, otherwise 
     *  <tt>false</tt>
     */
    boolean hasPendingMessage();

    /**
     * This method is only used for testing.  This sets the closed status of
     * the writer, specifying whether or not it should write messages to the
     * network.
     * 
     * @param closed the closed status of the connection
     */
    void setClosed(boolean closed);
}
