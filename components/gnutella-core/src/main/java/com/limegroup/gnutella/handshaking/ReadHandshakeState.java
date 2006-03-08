package com.limegroup.gnutella.handshaking;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;

import com.limegroup.gnutella.io.BufferReader;
import com.limegroup.gnutella.statistics.HandshakingStat;

/**
 * Superclass for HandshakeStates that are reading.
 */
abstract class ReadHandshakeState extends HandshakeState {
    /** Whether or not we've finished reading the initial connect line. */
    protected boolean doneConnect;
    /** The current header we're in the process of reading. */
    protected String currentHeader = "";
    /** The connect line. */
    protected String connectLine;
    
    /** Constructs a new ReadHandshakeState using the given HandshakeSupport. */
    ReadHandshakeState(HandshakeSupport support) {
        super(support);
    }

    /**
     * Reads as much data as it can from the buffer, farming the processing of the
     * connect line (same as response line) and headers out to the methods:
     *   processConnectLine(String line)
     *   processHeaders()
     *   
     * This will return true if it needs to be called again for more processing,
     * otherwise it will return false indiciating it's time to move on to the next
     * state.
     */
    boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        ReadableByteChannel rc = (ReadableByteChannel)channel;
        boolean allDone = false;
        while(!allDone) {
            int read = 0;
            
            while(buffer.hasRemaining() && (read = rc.read(buffer)) > 0);
            if(buffer.position() == 0) {
                if(read == -1)
                    throw new IOException("must read something when we don't have data!");
                break;
            }
            
            buffer.flip();
            BufferReader reader = new BufferReader(buffer);
            if(!doneConnect) {
                currentHeader += reader.readLine();
                if(!reader.isLineReadCompletely())
                    break;
                connectLine = currentHeader;
                currentHeader = "";
                processConnectLine();
                doneConnect = true;
            }
            
            if(doneConnect) {
                while(true) {
                    currentHeader += reader.readLine();
                    if(!reader.isLineReadCompletely())
                        break;
                    
                    if(!support.processReadHeader(currentHeader)) {
                        allDone = true;
                        break; // we finished reading this set of headers!
                    }
                    
                    currentHeader = ""; // reset for the next header.
                }
            }
            
            buffer.compact();
        }
        
        if(allDone) {
            processHeaders();
            return false;
        } else {
            return true;
        }
    }
    
    /** Returns false. */
    boolean isWriting() {
        return false;
    }
    
    /** Returns true. */
    boolean isReading() {
        return true;
    }
    
    /**
     * Reacts to the connect line, either throwing an IOException if it was invalid
     * or doing nothing if it was valid.
     */
    abstract void processConnectLine() throws IOException;
    
    /**
     * Reacts to the event of headers being finished processing.  Throws an IOException
     * if the connection wasn't allowed.
     * 
     * @throws IOException
     */
    abstract void processHeaders() throws IOException;
    
    /** The first state in an incoming handshake. */
    static class ReadRequestState extends ReadHandshakeState {
        ReadRequestState(HandshakeSupport support) {
            super(support);
        }

        /**
         * Ensures the initial connect line is GNUTELLA/0.6
         * or a higher version of the protocol.
         */
        void processConnectLine() throws IOException {
            if (!support.notLessThan06(connectLine))
                throw new IOException("not a valid connect string!");
        }

        /** Does nothing. */
        void processHeaders() throws IOException {}
    }
    
    /** The third state in an incoming handshake, or the second state in an outgoing handshake. */
    static class ReadResponseState extends ReadHandshakeState {
        ReadResponseState(HandshakeSupport support) {
            super(support);
        }
        
        /** Ensures that the connect line began with GNUTELLA/0.6 */
        void processConnectLine() throws IOException {
            if (!support.isConnectLineValid(connectLine)) {
                HandshakingStat.INCOMING_BAD_CONNECT.incrementStat();
                throw new IOException("Bad connect string");
            }
        }

        /** Ensures that the response contained a valid status code. */
        void processHeaders() throws IOException {
            HandshakeResponse theirResponse = support.createRemoteResponse(connectLine);
            switch(theirResponse.getStatusCode()) {
            case HandshakeResponse.OK:
                HandshakingStat.SUCCESSFUL_INCOMING.incrementStat();
                break;
            default:
                HandshakingStat.INCOMING_SERVER_UNKNOWN.incrementStat();
                throw NoGnutellaOkException.createServerUnknown(theirResponse.getStatusCode());
            }
        }        
    }    
}
