package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.util.Properties;

import com.limegroup.gnutella.handshaking.NoGnutellaOkException;

/**
 * This interface specifies the API for Gnutella connection handshaking.
 */
public interface Handshaker {

    /**
     * Performs the Gnutella connection hanshaking.  Depending on the 
     * underlying implementation, the completion of the handshake call may
     * or may not finish the handshake process.  For example, with non-blocking
     * IO, a call to this method will not necessarily complete the handshake.
     * 
     * @return <tt>true</tt> if the handshake successfully completed, otherwise
     *  <tt>false</tt>
     * @throws IOException if there is an IO error performing the hanshake
     * @throws NoGnutellaOkException if the remote host does not accept the 
     *  connection with a 200 OK status code
     * @return <tt>true</tt> if the connection successfully completed, otherwise
     *  <tt>false</tt>
     */
    public boolean handshake() throws IOException, NoGnutellaOkException;
    
    /**
     * Checks whether or not the handshaking process is complete.
     * 
     * @return <tt>true</tt> if the handshaking process has completed, 
     *  otherwise <tt>false</tt>
     */
    public boolean handshakeComplete();

    /**
     * Accessor for the headers that have been read from the remote host.
     * 
     * @return a <tt>Properties</tt> instance containing the headers that have
     *  been read from the remote host
     */
    public Properties getHeadersRead();

    /**
     * Accessor for the headers that we have written to the remote host.
     * 
     * @return a <tt>Properties</tt> instance containing the headers that we
     *  have written to the remote host
     */ 
    public Properties getHeadersWritten();

    /**
     * @param string
     * @return
     */
    public String getHeaderWritten(String string);

    /**
     * @return
     */
    public boolean write() throws IOException;

    /**
     * 
     */
    public void read() throws IOException;
}
