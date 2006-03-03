
// Commented for the Learning branch

package com.limegroup.gnutella.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;

import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.io.ChannelReadObserver;

/**
 * Reads uncompressed data from a remote computer, slices it into Gnutella packets, and hands them to the ManagedConnection that's sending us the data.
 * NIO realizes there's more uncompressed data for a MessageReader object to slice, it calls handleRead() on it.
 * 
 * You can make a new MessageReader object without giving it a source channel to read from.
 * If you do this, make sure you give it a channel with setReadChannel(channel) before something calls handleRead() on it.
 * To change the channel later, call setReadChannel(newchannel).
 * 
 * A channel's read() method returns 0 if there is no more data right now, and -1 if there will never be any more data, ever.
 * Getting the -1 is like hitting the end of a file.
 * The remote computer should keep sending us Gnutella packets until we disconnect, so a call to channel.read() should never return -1.
 * If it does, handleRead() throws an IOException with the text "eof".
 * 
 * Each time NIO calls handleRead(), this MessageReader will read as much data as it can from the source channel.
 * It will only stop when the source channel runs out of data.
 * 
 * MessageReader implements the ChannelReadObserver interface.
 * This interface combines the ChannelReader and ReadObserver interfaces.
 * ChannelReader means this object has a channel it can read from.
 * You can set it and get what you set with setReadChannel() and getReadChannel().
 * ReadObserver means this object can be told when it should read.
 * Code can call handleRead() on it to make it read now.
 * 
 * Extends and Implements
 * ChannelReader: This object has a source channel it reads from, setReadChannel() and getReadChannel().
 * ReadObserver:  NIO can tell this object to read now, handleRead().
 */
public class MessageReader implements ChannelReadObserver {

    /** The maximum size of a message payload that we'll accept, 64 KB. */
    private static final long MAX_MESSAGE_SIZE = 64 * 1024; // 64 KB
    /** A Gnutella packet header is 23 bytes. */
    private static final int HEADER_SIZE = 23;              // All Gnutella messages begin with a 23 byte header
    /** Look 19 bytes into the header to read the 4 byte payload size. */
    private static final int PAYLOAD_LENGTH_OFFSET = 19;    // The payload length is a number written in 4 bytes that begin 19 bytes into the header

    /** An empty buffer to use for packets that don't have payloads. */
    private static final ByteBuffer EMPTY_PAYLOAD = ByteBuffer.allocate(0);

    /** A 23 byte buffer we read a header into, and then look at it. */
    private final ByteBuffer header;
    /** The buffer for the payload, allocated to be exactly the right size for each packet. */
    private ByteBuffer payload;

    /**
     * The ManagedConnection object that represents the remote computer sending us data.
     * This is also the object we'll give the packets we slice back to.
     * 
     * ManagedConnection is the only class in LimeWire that implements the MessageReceiver interface.
     * This MessageReader class is the only one that referes to a ManagedConnection as a MessageReceiver.
     */
    private final MessageReceiver receiver;

    /** The source channel this MessageReader reads data from the remote computer through. */
    private ReadableByteChannel channel;

    /** True when this reader has been shut down, makes sure it doesn't go through the shutdown process more than once. */
    private boolean shutdown = false;

    /**
     * Make a new MessageReader object without giving it a source to read from yet.
     * Before you call handleRead() on your new object, you must call setReadChannel(ReadableByteChannel c) to give it a channel.
     * 
     * @param receiver The MessageReceiver this MessageReader should give packets to
     */
    public MessageReader(MessageReceiver receiver) {

    	// Call the constructor that takes a read channel, but give it null
        this(null, receiver);
    }

    /**
     * Make a new MessageReader object with the given source channel and MessageReceiver object
     * 
     * @param channel  The source channel this MessageReader object will read from
     * @param receiver The ManagedConnection object that the data is from, and which we'll give the sliced packets to
     */
    public MessageReader(ReadableByteChannel channel, MessageReceiver receiver) {

    	// Make sure the caller gave us a MessageReceiver, otherwise we'll have no object to give the messages
        if (receiver == null) throw new NullPointerException("null receiver");

        // Save the given channel and ManagedConnection in this object
        this.channel  = channel;
        this.receiver = receiver; // The ManagedConnection object the data we get is from, and that we'll give sliced packets to

        // Make the header buffer 23 bytes in little endian order
        this.header = ByteBuffer.allocate(HEADER_SIZE);
        header.order(ByteOrder.LITTLE_ENDIAN);

        // We'll make the payload buffer later
        this.payload = null;
    }

    /**
     * MessageReader implements the ChannelReadObserver interface.
     * This interface combines the ChannelReader and ReadObserver interfaces.
     * Implementing ChannelReader means this object has a channel it can read from.
     * Call setReadChannel(channel) to give this new MessageReeader object its channel.
     * 
     * The channel you give it is a ReadableByteChannel.
     * It might actually be a channel, like a socket we're downloading data from a remote computer through.
     * Or, it might be a special class that implements ReadableByteChannel, which just means it has a read(ByteBuffer b) method.
     * That custom read method can do anything.
     * It might read compressed data from another channel and decompress it first, for instance.
     * This is what InflaterReader does.
     * 
     * The ChannelReader interface requires this method.
     * Lets a caller set the channel this object will read from.
     * 
     * @param channel An object that implements ReadableByteChannel, that we can read from by calling read(ByteBuffer b)
     */
    public void setReadChannel(ReadableByteChannel channel) {

    	// Make sure the caller actually gave us something
        if (channel == null) throw new NullPointerException("cannot set null channel!");

        // Save the given channel in this object
        this.channel = channel;
    }

    /**
     * Gets the channel this object has been reading from.
     * This is the channel you set by calling setReadChannel(channel).
     * This object has been reading it by calling read(b) on it.
     * It's of type ReadableByteChannel, and might be a real channel, or just an object that has a read(b) method.
     * 
     * The ChannelReader interface requires this method.
     * Retrieves the channel this object has been reading from.
     * 
     * @return The channel this object has been reading from.
     */
    public ReadableByteChannel getReadChannel() {

    	// We saved it in our channel reference
        return channel;
    }

    /**
     * A call to handleRead() is a notification that a read can be performed on the channel.
     * Call handleRead() to make this MessageReader object read data, cut it into Gnutella packets, and pass them to the MessageReceiver.
     * 
     * Code somewhere else calls our handleRead() method to make us:
     * (1) Read data from our source.
     * (2) Split it into Gnutella packets.
     * (3) Give the packets to the MessageReceiver object.
     * 
     * The method reads from the channel with the line: read = channel.read(b);
     * The method hands a Gnutella packet to the MessageReceiver with the line: receiver.processReadMessage(m);
     * 
     * The ReadObserver interface requires this method.
     * Makes this object read and process data.
     */
    public void handleRead() throws IOException {

    	// Keep reading Gnutella packets and handing them to ManagedConnection.processReadMessage(m) until channel won't give us any more data
        while (true) {

        	// Count how many bytes we read from the source channel
        	int read = 0;

            // Loop until the header buffer is full or the source has no more data
            while (true) {

            	// The 23 byte header buffer is full, so it must contain a header
            	if (!header.hasRemaining()) break;

            	// Get decompressed data from the InflaterReader object
            	read = channel.read(header); // Put it in the header buffer, return the number of bytes we got

            	// The source couldn't give us any more data
            	if (read <= 0) break;
            }

            // If we couldn't fill the header buffer, we don't have the 23 bytes of a complete Gnutella packet header
            if (header.hasRemaining()) {

            	// The channel said we've reached its very end
                if (read == -1) throw new IOException("EOF"); // Report EOF, end of file

                // We ran our source out of data in the middle of a Gnutella packet header, stop trying for now
                break;
            }

            // At this point, we have a complete 23 byte Gnutella packet header in the header buffer

            // We haven't set up the payload buffer for this packet yet
            if (payload == null) {

            	// Read the payload length from the packet header
                int payloadLength = header.getInt(PAYLOAD_LENGTH_OFFSET); // Look 19 bytes into the header, and read 4 bytes there as an int

                // Make sure the length makes sense, the whole header might be mistake data from a confused remote computer
                if (payloadLength < 0 || payloadLength > MAX_MESSAGE_SIZE) throw new IOException("should i implement skipping?");

                // Some Gnutella packets don't have a payload
                if (payloadLength == 0) {

                	// Point payload at the empty buffer we made
                    payload = EMPTY_PAYLOAD;

                // This Gnutella packet has a payload
                } else {

                    try {

                    	// Allocate a new ByteBuffer exactly the right size to hold it
                        payload = ByteBuffer.allocate(payloadLength);

                    // This can run the computer out of memory
                    } catch (OutOfMemoryError e) { throw new IOException("message too large."); }
                }
            }

            // Now, we have a header in the header buffer and a new empty payload buffer that's exactly the right size

            // Loop until the payload buffer is full or the source has no more data
            while (true) {

            	// The payload buffer is full, meaning we've read the entire payload
            	if (!payload.hasRemaining()) break;

            	// Get decompressed data from the InflaterReader object
            	read = channel.read(payload);

            	// The source couldn't give us any more data
            	if (read <= 0) break;
            }
            
            // If we couldn't fill the perfectly sized payload buffer, we don't have this packet's complete payload yet
            if (payload.hasRemaining()) {

            	// The channel says we've reached its very end
            	if (read == -1) throw new IOException("EOF"); // Report EOF, end of file

            	// We ran our source out of dta in the middle of a Gnutella packet, stop trying for now
                break;
            }

            // The header and payload buffers hold a complete Gnutella packet
            try {

            	// Turn the data of the packet into a new Message object named m
                Message m = Message.createMessage( // This static factory method returns a new object or throws an exception
                    header.array(),                // Give it the data of the packet header and payload we just downloaded
                	payload.array(),
                	receiver.getSoftMax(),         // Ask the ManagedConnection object the maximum value for hops + TTL for this remote computer
                	receiver.getNetwork());        // Ask the ManagedConnection object whether this message came in through TCP or UDP

                // Call ManagedConnection.processReadMessage(m) to have it process the message that we just read
                receiver.processReadMessage(m);

            } catch (BadPacketException e) {}

            // We read a complete packet, and then the channel said we can never read any more from it
            if (read == -1) throw new IOException("eof"); // Report EOF, end of file

            // Get ready to read the next packet
            payload = null; // Let the garbage collector free the memory of the buffer we made for the packet payload
            header.clear(); // Mark the header buffer empty again, sets position at start and limit at end
        }
    }

    /** 
     * Frees the resources this object and its component objects were using.
     * The Shutdownable interface requires this method.
     */
    public void shutdown() {

    	// Make sure only one thread can get in here at a time
        synchronized (this) {

        	// Make sure this object only gets shut down once
            if (shutdown) return; // Already shut down, nothing more to do
            shutdown = true;      // Mark this object shut down so the next time we don't get here
        }

        // Tell the ManagedConnection object that NIO has shut us down
        receiver.messagingClosed();
    }

    /**
     * This method is not used.
     * 
     * The IOErrorObserver interface requires this method.
     * It makes this object able to handle IO exceptions.
     */
    public void handleIOException(IOException e) {

    	// Wrap the given IOException as a RuntimeException named "Unsupported", and throw it again
        throw new RuntimeException("unsupported operation", e);
    }
}
