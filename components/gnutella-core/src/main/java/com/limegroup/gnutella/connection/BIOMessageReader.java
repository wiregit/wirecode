package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;
import java.util.zip.Inflater;

import com.limegroup.gnutella.ByteOrder;
import com.limegroup.gnutella.MessageRouter;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.settings.MessageSettings;
import com.limegroup.gnutella.statistics.CompressionStat;
import com.limegroup.gnutella.statistics.ReceivedErrorStat;
import com.limegroup.gnutella.statistics.ReceivedMessageStatHandler;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.DataUtils;

/**
 * Class that takes care of reading messages from blocking sockets.
 */
public class BIOMessageReader extends AbstractMessageReader {
    
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
     * Cached soft max ttl -- if the TTL+hops is greater than SOFT_MAX,
     * the TTL is set to SOFT_MAX-hops.
     */
    public static final byte DEFAULT_SOFT_MAX = 
        ConnectionSettings.SOFT_MAX.getValue();
    
    /**
     * Creates a new <tt>BIOMessageReader</tt> for the specified 
     * <tt>Connection</tt> instance.
     * 
     * @return a new <tt>MessageReader</tt> for the specified connection
     */
    public static MessageReader createReader(Connection conn) {
        return new BIOMessageReader(conn);
    }
    
    /**
     * Creates a new <tt>BIOMessageReader</tt> associated with the specified
     * <tt>Connection</tt>.
     * 
     * @param conn the <tt>Connection</tt> instance this reader reads for
     */
    private BIOMessageReader(Connection conn) {
        super(conn);
        SOCKET = CONNECTION.getSocket();
        INFLATER = CONNECTION.getInflater();
        INPUT_STREAM = CONNECTION.getInputStream();
        SOFT_MAX = CONNECTION.getSoftMax();
    }
    

    /**
     * Implements the <tt>MessageReader</tt> interface.  This should never be
     * called on this class while in blocking mode, since any reading from
     * channels should be performed by <tt>NIOMessageReader</tt>.
     * 
     * @throws IllegalStateException if this method is ever called, since 
     *  this class should only be used in blocking mode
     */
    public Message createMessageFromTCP(SelectionKey key) {
        throw new IllegalStateException("in blocking mode");
    }

    /** A tiny allocation optimization; see Message.read(InputStream,byte[]). */
    private byte[] HEADER_BUF = new byte[23];
    
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
                msg = BIOMessageReader.read(INPUT_STREAM, HEADER_BUF, 
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

    /**
     * Reads a Gnutella message from the specified input stream.  The returned
     * message can be any one of the recognized Gnutella message, such as
     * queries, query hits, pings, pongs, etc.
     *
     * @param in the <tt>InputStream</tt> instance containing message data
     * @return a new Gnutella message instance
     * @throws <tt>BadPacketException</tt> if the message is not considered
     *  valid for any reason
     * @throws <tt>IOException</tt> if there is any IO problem reading the
     *  message
     */
    public static Message read(InputStream in)
        throws BadPacketException, IOException {
        return BIOMessageReader.read(in, new byte[23], Message.N_UNKNOWN, 
            DEFAULT_SOFT_MAX);
    }
    
    /**
     * Creates a new <tt>Message</tt> from an <tt>InputStream</tt> retrieved
     * from a UDP socket.  Some messages include flags for the transport layer,
     * making this specialized method necessary.<p>
     * 
     * Creates a new message unless one of the following happens:
     * 
     *    <ul>
     *    <li>No data is available: returns null
     *    <li>A bad packet is read: BadPacketException.  The client should be
     *      able to recover from this.
     *    <li>A major problem occurs: IOException.  This includes reading 
     *      packets that are ridiculously long and half-completed messages. The 
     *      client is not expected to recover from this.
     *    </ul>
     * 
     * @param in the <tt>InputStream</tt> to create the message from
     * @return a subclass of <tt>Message</tt>, or <tt>null</tt> if no data is
     *  available
     * @throws BadPacketException if we are not able to correctly parse the 
     *  data read from the network
     * @throws IOException if an IO error occurs or seriously abnormal packets
     *  are seen.  This will result in the connection being closed.
     */
    public static Message createMessageFromMulticast(InputStream in)
        throws BadPacketException, IOException {
        return BIOMessageReader.read(in, new byte[23], Message.N_MULTICAST, 
            DEFAULT_SOFT_MAX);
    } 
    
    /**
     * Creates a new <tt>Message</tt> from an <tt>InputStream</tt> retrieved
     * from a UDP socket.  Some messages include flags for the transport layer,
     * making this specialized method necessary.<p>
     * 
     * Creates a new message unless one of the following happens:
     * 
     *    <ul>
     *    <li>No data is available: returns null
     *    <li>A bad packet is read: BadPacketException.  The client should be
     *      able to recover from this.
     *    <li>A major problem occurs: IOException.  This includes reading 
     *      packets that are ridiculously long and half-completed messages. The 
     *      client is not expected to recover from this.
     *    </ul>
     * 
     * @param in the <tt>InputStream</tt> to create the message from
     * @return a subclass of <tt>Message</tt>, or <tt>null</tt> if no data is
     *  available
     * @throws BadPacketException if we are not able to correctly parse the 
     *  data read from the network
     * @throws IOException if an IO error occurs or seriously abnormal packets
     *  are seen.  This will result in the connection being closed.
     */
    public static Message createMessageFromUDP(InputStream in)
        throws BadPacketException, IOException {
        return BIOMessageReader.read(in, new byte[23], Message.N_UDP, 
            DEFAULT_SOFT_MAX);
    }     

        
    /**
     * @param network the network this was received from.
     * @requires buf.length==23
     * @effects exactly like Message.read(in), but buf is used as scratch for
     *  reading the header.  This is an optimization that lets you avoid
     *  repeatedly allocating 23-byte arrays.  buf may be used when this returns,
     *  but the contents are not guaranteed to contain any useful data.  
     */
    public static Message read(InputStream in, byte[] buf, int network, 
        byte softMax)
        throws BadPacketException, IOException {

        //1. Read header bytes from network.  If we timeout before any
        //   data has been read, return null instead of throwing an
        //   exception.
        for (int i=0; i<23; ) {
            int got;
            try {
                got = in.read(buf, i, 23-i);
            } catch (InterruptedIOException e) {
                //have we read any of the message yet?
                if (i==0) return null;
                else throw e;
            }
            if (got==-1) {
                if( RECORD_STATS ){
                    ReceivedErrorStat.CONNECTION_CLOSED.incrementStat();
                }
                throw new IOException("Connection closed.");
            }
            i+=got;
        }

        //2. Unpack.
        byte func = buf[16];
        byte ttl = buf[17];
        byte hops = buf[18];
        int length = ByteOrder.leb2int(buf,19);
        //2.5 If the length is hopelessly off (this includes lengths >
        //    than 2^31 bytes, throw an irrecoverable exception to
        //    cause this connection to be closed.
        if (length<0 || length > MessageSettings.MAX_LENGTH.getValue()) {
            if( RECORD_STATS )
                ReceivedErrorStat.INVALID_LENGTH.incrementStat();
            throw new IOException("Unreasonable message length: "+length);
        }

        //3. Read rest of payload.  This must be done even for bad
        //   packets, so we can resume reading packets.
        byte[] payload = null;
        if (length!=0) {
            payload = new byte[length];
            for (int i=0; i<length; ) {
                int got=in.read(payload, i, length-i);
                if (got==-1) {
                    if( RECORD_STATS )
                        ReceivedErrorStat.CONNECTION_CLOSED.incrementStat();
                    throw new IOException("Connection closed.");
                }
                i+=got;
            }
        }
        else {
            payload = DataUtils.EMPTY_BYTE_ARRAY;
        }

        ttl = checkFields(ttl, hops, softMax, func);
        
        // Delayed GUID allocation
        byte[] guid = new byte[16];
        for (int i=0; i<16; i++) //TODO3: can optimize
            guid[i] = buf[i];
            
        return createMessage(guid, func, ttl, hops, length, payload, network);
    }

    /**
     * Starts reading messages from this TCP connection.
     * 
     * @throws IOException if there is an IO error reading from this socket
     */
    public void startReading() throws IOException {
        while (true) {
            Message msg = null;
            try {
                msg = read();
                if (msg == null)
                    continue;
            } catch (BadPacketException e) {
                // Don't increment any message counters here.  It's as if
                // the packet never existed
                continue;
            }
    
            routeMessage(msg);
        }
    }
}
