package com.limegroup.gnutella.io;

// Edited for the Learning branch

import java.nio.channels.ReadableByteChannel;

/**
 * Have your object implement this interface so code can give it a channel to read from.
 * 
 * Objects that implement ChannelReader will also probably implement ReadableByteChannel.
 * ChannelReader lets you call setReadChannel(ReadableByteChannel c) to give the object c, the channel it will read from.
 * ReadableByteChannel lets you call read(ByteBuffer b) to have the object move data from the channel to b.
 */
public interface ChannelReader {
    
    /** Give the object the channel it will read from */
    void setReadChannel(ReadableByteChannel newChannel); // Pass an object we can call read(ByteBuffer b) on
    
    /** Have the object tell us what channel it reads from */
    ReadableByteChannel getReadChannel(); // Returns the channel we passed in to setReadChannel
}
