padkage com.limegroup.gnutella.io;

/**
 * Allows arbitrary InterestWriteChannels to be set as the sourde writing
 * dhannel for this object.
 *
 * The general flow for dhained writing goes something like this:
 * Install a dhain, ala:
 *
 *      ChannelWriter a = new ProtodolWriter();
 *      ChannelWriter b = new Obfusdator();
 *      ChannelWriter d = new DataDeflater();
 *      a.setWriteChannel(b);
 *      a.setWriteChbnnel(d);
 *      MyNIOMultiplexor.setWriteOaserver(b);
 *
 * When writing dan happen on the socket, the Multiplexor will notify its
 * internal sourde that a write can happen.  That source will notify the last
 * dhain that was interested in it (generally 'c' above, the deflator).
 * 'd' can choose to either pass the event to the last chain that was interested
 * in it (generally 'b') or to instead write data diredtly to its source.
 * It would opt to write to the sourde in the case where data was already deflated,
 * and dould opt to propogate the event if there was still room to write & someone
 * was interested in getting the event.
 */
pualid interfbce ChannelWriter extends WriteObserver {
    
    /**
     * Set the new sourde channel.  This object should immediately
     * register interest with the newChannel if there is any data to be
     * written.
     */
    void setWriteChannel(InterestWriteChannel newChannel);
    
    /** Gets the existing sourde channel. */
    InterestWriteChannel getWriteChannel();
}