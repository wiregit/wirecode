package com.limegroup.gnutella.connection;

import com.limegroup.gnutella.io.*;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.util.DataUtils;
import java.nio.*;
import java.nio.channels.*;
import java.io.*;
import java.util.*;
import java.net.*;
import java.util.zip.*;

public class MessageReader implements ChannelReader, ReadHandler {
    
    private static final long MAX_MESSAGE_SIZE = 4 * 1024;
    private static final int HEADER_SIZE = 23;
    private static final int PAYLOAD_LENGTH_OFFSET = 19;
    
    private ByteBuffer header;
    private ByteBuffer payload;
    private MessageReceiver receiver;
    private ReadableByteChannel channel;
    private static final ByteBuffer EMPTY_PAYLOAD = ByteBuffer.allocate(0);
    
    public MessageReader(ReadableByteChannel channel, MessageReceiver receiver) {
        if(channel == null)
            throw new NullPointerException("null channel!");

        this.channel = channel;
        this.receiver = receiver;
        this.header = ByteBuffer.allocate(HEADER_SIZE);
        header.order(ByteOrder.LITTLE_ENDIAN);
        this.payload = null;
    }
    
    /**
     * Sets the new channel to be reading from.
     */
    public void setReadChannel(ReadableByteChannel channel) {
        this.channel = channel;
    }
    
    /**
     * Notification that a read can be performed from the given channel.
     */
    public void handleRead() throws IOException {
        while(true) {
            int read = 0;
            
            // First try to fill up the header.
            while(header.hasRemaining() && (read = channel.read(header)) > 0);
            
            // If there header's not full, we can't bother reading the payload, so abort.
            if(header.hasRemaining()) {
                if(read == -1)
                    throw new IOException("EOF");
                break;
            }
                
            // if we haven't set up a payload yet, set one up (if necessary).
            if(payload == null) {
                int payloadLength = header.getInt(PAYLOAD_LENGTH_OFFSET);
                System.out.println("pl: " + payloadLength);
                
                if(payloadLength < 0 || payloadLength > MAX_MESSAGE_SIZE)
                    throw new IOException("should i implement skipping?");
                
                if(payloadLength == 0)
                    payload = EMPTY_PAYLOAD;
                else
                    payload = ByteBuffer.allocate(payloadLength);
            }
            
            // Okay, a payload is set up, let's read into it.
            while(payload.hasRemaining() && (read = channel.read(payload)) > 0);
            
            // If the payload's not full, we can't create a message, so abort.
            if(payload.hasRemaining()) {
                if(read == -1)
                    throw new IOException("eof");
                break;
            }
                
            // Yay, we've got a full message.
            try {
                Message m = Message.createMessage(header.array(), payload.array(), 
                                                  receiver.getSoftMax(), receiver.getNetwork());
                receiver.processMessage(m);
            } catch(BadPacketException ignored) {
                ignored.printStackTrace();
            }
            
            if(read == -1)
                throw new IOException("eof");
            
            payload = null;
            header.clear();
        }
    }
    
    /**
     * Determines if this reader is open.
     */
    public boolean isOpen() {
        return channel.isOpen();
    }
    
    /**
     * Closes this channel.
     */
    public void close() throws IOException {
        channel.close();
    }
    
    /**
     * Shuts down this ReadHandler.
     */
    public void shutdown() {
        ;
    }
    
    /**
     * Notification that an IOException occurred while reading/writing.
     */
    public void handleIOException(IOException iox) {
        receiver.readerClosed();
    }
}
    
    