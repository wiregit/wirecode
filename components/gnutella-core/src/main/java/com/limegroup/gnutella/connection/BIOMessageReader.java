package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.util.zip.Inflater;

import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.statistics.CompressionStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.util.CommonUtils;

/**
 * Class that takes care of reading messages from blocking sockets.
 */
public class BIOMessageReader extends AbstractMessageReader {

    private final Connection CONNECTION;
    
    /**
     * Cache the 'connection closed' exception, so we have to allocate
     * one for every closed connection.
     */
    private static final IOException CONNECTION_CLOSED =
        new IOException("connection closed");
        
    /**
     * Constant for the socket for this connection.
     */    
    private final Socket SOCKET;
    
    /**
     * Constant for the infator for this connection that inflates compressed
     * streams.
     */
    private final Inflater INFLATER;
    
    /**
     * Constant for the input stream for this connection.
     */
    private final InputStream INPUT_STREAM;
    
    /**
     * Constant for the soft max ttl for this connection.
     */
    private final byte SOFT_MAX;
    
    /**
     * @return
     */
    public static MessageReader createReader(Connection conn) {
        // TODO Auto-generated method stub
        return new BIOMessageReader(conn);
    }
    
    private BIOMessageReader(Connection conn) {
        CONNECTION = conn;
        SOCKET = CONNECTION.getSocket();
        INFLATER = CONNECTION.getInflater();
        INPUT_STREAM = CONNECTION.getInputStream();
        SOFT_MAX = CONNECTION.getSoftMax();
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageReader#createMessageFromTCP(java.io.InputStream)
     */
    public Message createMessageFromTCP(InputStream is) 
        throws BadPacketException, IOException {
        return Message.read(is);
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageReader#createMessageFromTCP(java.nio.channels.SelectionKey)
     */
    public Message createMessageFromTCP(SelectionKey key) {
        // TODO Auto-generated method stub
        return null;
    }

    /** A tiny allocation optimization; see Message.read(InputStream,byte[]). */
    private byte[] HEADER_BUF=new byte[23];
    
    /**
     * Receives a message.  This method is NOT thread-safe.  Behavior is
     * undefined if two threads are in a receive call at the same time for a
     * given connection.
     *
     * @requires this is fully initialized
     * @effects exactly like Message.read(), but blocks until a
     *  message is available.  A half-completed message
     *  results in InterruptedIOException.
     */
    public Message read() throws IOException, BadPacketException {
        try {
            //On the Macintosh, sockets *appear* to return the same ping reply
            //repeatedly if the connection has been closed remotely.  This 
            //prevents connections from dying.  The following works around the 
            //problem.  Note that Message.read may still throw IOException 
            //below.  See note on _closed for more information.
            if (!CONNECTION.isOpen()) {
                throw CONNECTION_CLOSED;
            }
    
            Message msg = null;
            while (msg == null) {
                msg = readAndUpdateStatistics();
            }
            // record received message in stats
            CONNECTION.stats().addReceived();
            return msg;
        } catch(IOException e) {
            RouterService.removeConnection(CONNECTION);
            throw e;
        }
    }

    /**
     * Receives a message with timeout.  This method is NOT thread-safe.
     * Behavior is undefined if two threads are in a receive call at the same
     * time for a given connection.  THIS METHOD IS ONLY USED FOR TESTING.
     *
     * @requires this is fully initialized
     * @effects exactly like Message.read(), but throws InterruptedIOException
     *  if timeout!=0 and no message is read after "timeout" milliseconds.  In
     *  this case, you should terminate the connection, as half a message may
     *  have been read.
     */
    public Message read(int timeout)
        throws IOException, BadPacketException, InterruptedIOException {
        //See note in receive().
        if (!CONNECTION.isOpen()) {
            throw CONNECTION_CLOSED;
        }

        //temporarily change socket timeout.
        int oldTimeout = SOCKET.getSoTimeout();
        SOCKET.setSoTimeout(timeout);
        try {
            Message m = readAndUpdateStatistics();
            if (m==null) {
                throw new InterruptedIOException("null message read");
            }
            
            // record received message in stats
            CONNECTION.stats().addReceived();
            return m;
        } finally {
            SOCKET.setSoTimeout(oldTimeout);
        }
    }
    
    /**
     * Reads a message from the network and updates the appropriate statistics.
     */
    private Message readAndUpdateStatistics()
      throws IOException, BadPacketException {
        int pCompressed = 0, pUncompressed = 0;
        
        // The try/catch block is necessary for two reasons...
        // See the notes in Connection.close above the calls
        // to end() on the Inflater/Deflater and close()
        // on the Input/OutputStreams for the details.
        Message msg = null;
        try {
            if(CONNECTION.isReadDeflated()) {
                pCompressed = INFLATER.getTotalIn();
                pUncompressed = INFLATER.getTotalOut();
            }
            
            // DO THE ACTUAL READ
            try {
                msg = Message.read(INPUT_STREAM, HEADER_BUF, 
                    Message.N_TCP, SOFT_MAX);
            } catch(IOException e) {
                CONNECTION.close(); // if IOError, make sure we close.
                throw e;
            }
            
            // _bytesReceived must be set differently
            // when compressed because the inflater will
            // read more input than a single message,
            // making it appear as if the deflated input
            // was actually larger.
            if(CONNECTION.isReadDeflated() ) {
                CONNECTION.stats().addCompressedBytesReceived(INFLATER.getTotalIn());
                CONNECTION.stats().addBytesReceived(INFLATER.getTotalOut());
                if(!CommonUtils.isJava118()) {
                    CompressionStat.GNUTELLA_UNCOMPRESSED_DOWNSTREAM.addData(
                        (INFLATER.getTotalOut() - pUncompressed));
                    CompressionStat.GNUTELLA_COMPRESSED_DOWNSTREAM.addData(
                        (INFLATER.getTotalIn() - pCompressed));
                }            
            } else if(msg != null) {
                CONNECTION.stats().addBytesReceived(msg.getTotalLength());
            }
        } catch(NullPointerException npe) {
            throw CONNECTION_CLOSED;
        }
        return msg;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.connection.MessageReader#startReading()
     */
    public void startReading() throws IOException {
        MessageRouter router = RouterService.getMessageRouter();
        while (true) {
            Message m = null;
            try {
                m = read();
                if (m==null)
                    continue;
            } catch (BadPacketException e) {
                // Don't increment any message counters here.  It's as if
                // the packet never existed
                continue;
            }
    
            // Run through the route spam filter and drop accordingly.
            if (CONNECTION.isSpam(m)) {
                if(!CommonUtils.isJava118()) {
                    ReceivedMessageStatHandler.TCP_FILTERED_MESSAGES.
                        addMessage(m);
                }
                CONNECTION.stats().countDroppedMessage();
                continue;
            }
    
            //call MessageRouter to handle and process the message
            router.handleMessage(m, CONNECTION);            
        }
    }
}
