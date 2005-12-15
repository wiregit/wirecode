
// Edited for the Learning branch

package com.limegroup.gnutella.io;

/**
 * You have a channel you write to, setWriteChannel() and getWriteChannel().
 * NIO can command you to get data and write, handleWrite().
 * 
 * 
 * Allows arbitrary InterestWriteChannels to be set as the source writing
 * channel for this object.
 *
 * The general flow for chained writing goes something like this:
 * Install a chain, ala:
 *
 *      ChannelWriter a = new ProtocolWriter();
 *      ChannelWriter b = new Obfuscator();
 *      ChannelWriter c = new DataDeflater();
 *      a.setWriteChannel(b);
 *      b.setWriteChannel(c);
 *      MyNIOMultiplexor.setWriteObserver(a);
 *
 * When writing can happen on the socket, the Multiplexor will notify its
 * internal source that a write can happen.  That source will notify the last
 * chain that was interested in it (generally 'c' above, the deflator).
 * 'c' can choose to either pass the event to the last chain that was interested
 * in it (generally 'b') or to instead write data directly to its source.
 * It would opt to write to the source in the case where data was already deflated,
 * and could opt to propogate the event if there was still room to write & someone
 * was interested in getting the event.
 * 
 * (added)
 * An object that implements ChannelWriter has a channel it can write to.
 * You can call write(ByteBuffer b) on the object to give it data.
 * You can call setWriteChannel to give it a channel, and getWriteChannel to find what you set.
 * The channel you give it must support the InterestWriteChannel interface.
 */
public interface ChannelWriter extends WriteObserver {
    
    /**
     * Set the new source channel.  This object should immediately
     * register interest with the newChannel if there is any data to be
     * written.
     */
    void setWriteChannel(InterestWriteChannel newChannel);
    
    /** Gets the existing source channel. */
    InterestWriteChannel getWriteChannel();
}