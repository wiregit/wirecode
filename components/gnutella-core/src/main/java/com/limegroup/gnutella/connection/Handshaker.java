package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;

import com.limegroup.gnutella.handshaking.HandshakeResponse;
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
     * Determines whether or not header reading is complete for this handshaker.
     * This is primarily used for non-blocking handshakes to determine 
     * whether to read handshake headers or Gnutella messages.
     * 
     * @return <tt>true</tt> if handshake reading is complete, otherwise 
     *  <tt>false</tt>
     */
    public boolean readComplete();
    
    /**
     * Determines whether or not handshake writing is complete.  This is 
     * used particularly for non-blocking IO when determining whether we 
     * should write handshake headers or Gnutella message data.
     * 
     * @return <tt>true</tt> if all handshake data has been written, otherwise
     *  <tt>false</tt>
     */
    public boolean writeComplete();

    /**
     * Accessor for the headers that have been read from the remote host.
     * 
     * @return a <tt>HandshakeResponse</tt> instance containing the headers 
     *  that have been read from the remote host
     */
    public HandshakeResponse getHeadersRead();

    /**
     * Accessor for the headers that we have written to the remote host.
     * 
     * @return a <tt>Properties</tt> instance containing the headers that we
     *  have written to the remote host
     */ 
    public Properties getHeadersWritten();

    /**
     * Accessor for the specified header written to the remote host.
     * 
     * @param name the name for the handshake header to access
     * @return the value associated with the specified header name
     */
    public String getHeaderWritten(String name);

    /**
     * Writes handshake data to the remote host.
     * 
     * @return <tt>true</tt> if all available data was successfully written,
     *  otherwise <tt>false</tt>
     * @throws IOException if an IO error occurs during writing
     */
    public boolean write() throws IOException;

    /**
     * Reads any available handshake data from the remote host.
     * 
     * @throws IOException if an IO error occurs during reading
     */
    public void read() throws IOException;

    /**
     * Accessor for any leftover Gnutella message data that was read at the 
     * end of handshaking.  This is particularly relevant to non-blocking
     * handshakes where we simply fill a buffer with available data on reads.
     * For INCOMING connections, we can also read some Gnutella message data
     * when we read the end of the handshake.  This method provides access to
     * that extra data so that we can properly create Gnutella messages out of
     * it.
     * 
     * @return the <tt>ByteBuffer</tt> containing any remaining Gnutella message
     *  data from the connection handshake.  The returned <tt>ByteBuffer</tt>
     *  is guaranteed to be non-null, although it will often not contain any
     *  extra data
     */
    public ByteBuffer getRemainingData();
}
