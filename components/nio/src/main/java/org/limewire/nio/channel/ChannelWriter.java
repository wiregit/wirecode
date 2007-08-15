package org.limewire.nio.channel;

import org.limewire.nio.observer.WriteObserver;

/**
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
 */
public interface ChannelWriter extends WriteObserver {
    
    /**
     * Set the new source channel.  This object should immediately
     * register interest with the newChannel if there is any data to be
     * written.
     */
    void setWriteChannel(InterestWritableByteChannel newChannel);
    
    /** Gets the existing source channel. */
    InterestWritableByteChannel getWriteChannel();
}