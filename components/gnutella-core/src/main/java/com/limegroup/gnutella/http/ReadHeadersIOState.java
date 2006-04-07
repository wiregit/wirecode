package com.limegroup.gnutella.http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;

import com.limegroup.gnutella.io.BufferUtils;
import com.limegroup.gnutella.io.IOState;
import com.limegroup.gnutella.settings.ConnectionSettings;
import com.limegroup.gnutella.statistics.Statistic;

public abstract class ReadHeadersIOState implements IOState {

    /** Header support. */
    protected final HeaderSupport support;
    /** Statistic to add bandwidth data to. */
    private final Statistic stat;
    /** Whether or not we've finished reading the initial connect line. */
    protected boolean doneConnect;
    /** The current header we're in the process of reading. */
    protected StringBuffer currentHeader = new StringBuffer(1024);
    /** The connect line. */
    protected String connectLine;
    
    /** Constructs a new ReadHandshakeState using the given support & stat. */
    public ReadHeadersIOState(HeaderSupport support, Statistic stat) {
        this.support = support;
        this.stat = stat;
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
    public boolean process(Channel channel, ByteBuffer buffer) throws IOException {
        ReadableByteChannel rc = (ReadableByteChannel)channel;
        boolean allDone = false;
        while(!allDone) {
            int read = 0;
            
            while(buffer.hasRemaining() && (read = rc.read(buffer)) > 0)
                stat.addData(read);
            
            if(buffer.position() == 0) {
                if(read == -1)
                    throw new IOException("EOF");
                break;
            }
            
            buffer.flip();
            if(!doneConnect) {
                if(BufferUtils.readLine(buffer, currentHeader)) {
                    connectLine = currentHeader.toString();
                    System.out.println("cl: " + connectLine);
                    currentHeader.delete(0, currentHeader.length());
                    processConnectLine();
                    doneConnect = true;
                }
            }
            
            if(doneConnect) {
                while(true) {
                    if(!BufferUtils.readLine(buffer, currentHeader))
                        break;
                    
                    if(!support.processReadHeader(currentHeader.toString())) {
                        allDone = true;
                        break; // we finished reading this set of headers!
                    }
                    
                    currentHeader.delete(0, currentHeader.length()); // reset for the next header.

                    // Make sure we don't try and read forever.
                    if(support.getHeadersReadSize() > ConnectionSettings.MAX_HANDSHAKE_HEADERS.getValue())
                        throw new IOException("too many headers");
                }
            }
            
            buffer.compact();
            
            // Don't allow someone to send us a header so big that we blow up.
            // Note that we don't check this after immediately after creating the
            // header, because it's not really so important there.  We know the
            // data cannot be bigger than the buffer's size, and the buffer's size isn't
            // too extraordinarily large, so this works out okay.
            if(currentHeader.length() > ConnectionSettings.MAX_HANDSHAKE_LINE_SIZE.getValue())
                throw new IOException("header too big");
        }
        
        if(allDone) {
            processHeaders();
            return false;
        } else {
            return true;
        }
    }
    
    /** Returns false. */
    public boolean isWriting() {
        return false;
    }
    
    /** Returns true. */
    public boolean isReading() {
        return true;
    }
    
    /**
     * Reacts to the connect line, either throwing an IOException if it was invalid
     * or doing nothing if it was valid.
     */
    abstract protected void processConnectLine() throws IOException;
    
    /**
     * Reacts to the event of headers being finished processing.  Throws an IOException
     * if the connection wasn't allowed.
     * 
     * @throws IOException
     */
    abstract protected void processHeaders() throws IOException;

}
