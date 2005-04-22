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

public class MessageReader implements WritableByteChannel {
    
    private static final int MAX_MESSAGE_SIZE = 4 * 1024;
    private static final int HEADER_SIZE = 23;
    private static final int PAYLOAD_LENGTH_OFFSET = 19;
    
    private ByteBuffer buffer;
    private MessageReceiver receiver;
    
    public MessageReader(MessageReceiver receiver) {
        this.receiver = receiver;
        this.buffer = ByteBuffer.wrap(new byte[MAX_MESSAGE_SIZE + HEADER_SIZE]);
    }
    
    /**
     * Translates the inBuffer into Messages, dispatching them to the receiver.
     */
    public int write(ByteBuffer inBuffer) throws IOException {
        if(buffer.position() == 0)
            return read(inBuffer);
        else if(inBuffer.hasRemaining())
            return read(buffer, inBuffer);
        else //no data in either storage or inBuffer.
            return 0;
    }
    
    /**
     * Creates a message from a a buffer.
     * If a message cannot be fully read, moves the buffer into our storage buffer
     * (if it wasn't the storage buffer already).
     */
    private int read(ByteBuffer buf) throws IOException {
        System.out.println("reading off of new buffer");
        
        
        int numRead = 0;
        int totalRead = 0;
        while(buf.hasRemaining()) {
            int position = buf.position();
            int limit = buf.limit();
            int length = limit - position;
    
            // cannot deal if the buffer is less than the header.
            if(length < HEADER_SIZE) {
                System.out.println("not enough data to read header.  buffer: " + buf);
                if(buffer != buf)
                    buffer.put(buf);
                totalRead += length;
                break;
            }
            
            // okay, we have enough data in the header, see what the length of the msg is.
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int payloadLength = buf.getInt(PAYLOAD_LENGTH_OFFSET + position);
            buf.order(ByteOrder.BIG_ENDIAN);
            if(payloadLength > MAX_MESSAGE_SIZE)
                throw new IOException("message too large. cannot deal.");
            
            int endOfPayload = payloadLength + HEADER_SIZE;
            // we don't have the whole message here, pack it up for later.
            if(endOfPayload > length) {
                System.out.println("not enough data to read payload ( " + payloadLength + "), buffer: " + buf);
                if(buffer != buf)
                    buffer.put(buf);
                totalRead += length;
                break;
            }
            
            // okay, we've got the whole message here, let's take the header & payload.
            byte[] header = buf.array(); // we don't need to trim, we can reference directly.
            byte[] payload;
            
            if(payloadLength == 0) {
                payload = DataUtils.EMPTY_BYTE_ARRAY;
            } else {
                payload = new byte[payloadLength];
                System.arraycopy(header, HEADER_SIZE + position, payload, 0, payload.length);
                buf.position(endOfPayload + position);
            }
            numRead++;
            totalRead += endOfPayload;
            
            try {
                Message m = Message.createMessage(header, position, payload, receiver.getSoftMax(), receiver.getNetwork());
                receiver.processMessage(m);
            } catch(BadPacketException ignored) {
                ignored.printStackTrace();
            }
        }
        
        System.out.println("read " + numRead + " messages.  storage now: " + buffer + ", new buffer: " + buf);
        
        return totalRead;
    }
    
    // byte[] to use when reading headers -- possible to cache this because
    // reading is all done in a single thread, and headers aren't re-used.
    private final byte[] COMBINED_HEADER = new byte[19];
    
    /**
     * Creates a message, reading first from our storage buffer & then from the new buffer.
     */
    private int read(ByteBuffer storage, ByteBuffer newBuffer) throws IOException {        
        int totalRead = 0;
        storage.flip();
        
        System.out.println("reading off both buffers.  storage: " + storage + ", new: " + newBuffer);
        
        if(!storage.hasRemaining())
            throw new IllegalStateException("no data in storage.");
        if(!newBuffer.hasRemaining())
            throw new IllegalStateException("no data in newBuffer");
            
        int sPos = storage.position();
        int sLim = storage.limit();
        int sLen = sLim - sPos;
        
        int nPos = newBuffer.position();
        int nLim = newBuffer.limit();
        int nLen = nLim - nPos;

        // if there wasn't enough combined data to even read a header, 
        // put new data in the storage & exit.
        if(nLen + sLen < HEADER_SIZE) {
            storage.compact();
            storage.put(newBuffer);
            System.out.println("combined lenghts (storage: " + sLen + ", new: " + nLen + ") less than header size.  storage: " + storage + ", buffer: " + newBuffer);
            totalRead += nLen;
        } else {
            
            int payloadLength;
            
            // figure out the payload length.
            // a) it may be in the storage,
            // b) it may be in the new buffer.
            // or c) it may be in both.
            if(sLen >= PAYLOAD_LENGTH_OFFSET + 4) {
                storage.order(ByteOrder.LITTLE_ENDIAN);
                payloadLength = storage.getInt(PAYLOAD_LENGTH_OFFSET + sPos);
                storage.order(ByteOrder.BIG_ENDIAN);   
            } else if(sLen <= PAYLOAD_LENGTH_OFFSET) {
                newBuffer.order(ByteOrder.LITTLE_ENDIAN);
                payloadLength = newBuffer.getInt(PAYLOAD_LENGTH_OFFSET - sLen + nPos);
                newBuffer.order(ByteOrder.BIG_ENDIAN);                
            } else {
                byte b1, b2, b3, b4;
                switch(sLen) {
                case PAYLOAD_LENGTH_OFFSET + 1:
                    b1 = storage.get(PAYLOAD_LENGTH_OFFSET + sPos);
                    b2 = newBuffer.get(0 + nPos);
                    b3 = newBuffer.get(1 + nPos);
                    b4 = newBuffer.get(2 + nPos);
                    break;
                case PAYLOAD_LENGTH_OFFSET + 2:
                    b1 = storage.get(PAYLOAD_LENGTH_OFFSET + sPos);
                    b2 = storage.get(PAYLOAD_LENGTH_OFFSET + 1 + sPos);
                    b3 = newBuffer.get(0 + nPos);
                    b4 = newBuffer.get(1 + nPos);
                    break;
                case PAYLOAD_LENGTH_OFFSET + 3:
                    b1 = storage.get(PAYLOAD_LENGTH_OFFSET + sPos);
                    b2 = storage.get(PAYLOAD_LENGTH_OFFSET + 1 + sPos);
                    b3 = storage.get(PAYLOAD_LENGTH_OFFSET + 2 + sPos);
                    b4 = newBuffer.get(0 + nPos);
                    break;
                default:
                    throw new IllegalStateException("impossible");
                }
                payloadLength = ( (int)b1 & 0xFF       ) |
                                (((int)b2 & 0xFF) <<  8) |
                                (((int)b3 & 0xFF) << 16) |
                                ( (int)b4         << 24);
            }
            
            if(payloadLength > MAX_MESSAGE_SIZE)
                throw new IOException("unreasonably large message length.");
                
            if(payloadLength + HEADER_SIZE > nLen + sLen) {
                storage.compact();
                storage.put(newBuffer);
                totalRead += nLen;
            } else {
                
                byte[] header;
                int headerOffset;
                byte[] payload;
                
                if(payloadLength == 0)
                    payload = DataUtils.EMPTY_BYTE_ARRAY;
                else
                    payload = new byte[payloadLength];
                    
                if(sLen  >= COMBINED_HEADER.length) {
                    header = storage.array();
                    headerOffset = 0;
                    if(sLen <= HEADER_SIZE) { // can retrieve payload only from newBuffer.
                        System.arraycopy(newBuffer.array(), HEADER_SIZE - sLen + nPos, payload, 0, payload.length);
                    } else { // payload is somewhat in storage & somewhat in newBuffer.
                        int startOfDataInStorage = HEADER_SIZE + sPos;
                        int amountOfDataInStorage = sLen - HEADER_SIZE;
                        System.out.println("startDIS: " + startOfDataInStorage + ", amountDIS: " + amountOfDataInStorage + ", nPos: " + nPos + ", p.length: " + payload.length);
                        System.arraycopy(storage.array(), startOfDataInStorage, payload, 0, amountOfDataInStorage);
                        System.arraycopy(newBuffer.array(), nPos, payload, amountOfDataInStorage, payload.length - amountOfDataInStorage);
                    }
                } else {
                    header = COMBINED_HEADER;
                    headerOffset = 0;
                    System.arraycopy(storage.array(), sPos, header, 0, sLen);
                    System.arraycopy(newBuffer.array(), nPos, header, sLen, header.length - sLen);
                    if(payloadLength != 0)
                        System.arraycopy(newBuffer.array(), HEADER_SIZE - sLen + nPos, payload, 0, payload.length);
                }
                
                totalRead += payloadLength + HEADER_SIZE - sLen;
                try {
                    Message m = Message.createMessage(header, headerOffset, payload, receiver.getSoftMax(), receiver.getNetwork());
                    receiver.processMessage(m);
                } catch(BadPacketException ignored) {}
                
                storage.limit(storage.capacity());
                storage.position(0);
                newBuffer.position(payloadLength + HEADER_SIZE - sLen + nPos);
                totalRead += read(newBuffer);
            }
        }
        
        return totalRead;
    }
    
    /**
     * Determines if this reader is open.
     */
    public boolean isOpen() {
        return true;
    }
    
    /**
     * Closes this channel.
     */
    public void close() throws IOException {
        buffer = null;
        receiver.readerClosed();
    }
}
    
    