
// Commented for the Learning branch

package com.limegroup.gnutella.connection;

/**
 * Counts the number of Gnutella packets we've sent, received, and dropped in our communications with a remote computer running Gnutella software.
 * 
 * Call addSent() when you release a packet into the MessageWriter.
 * Call addSentDropped() when the MessageWriter tells you how many packets it dropped to save bandwidth.
 * 
 * Call addReceived() when you receive a packet.
 * Call addReceivedDropped() when the SpamFilter or MessageRouter drops it.
 * 
 * When LimeWire runs, there is one ConnectionStats object for each computer we're connected to.
 * ManagedConnection makes a new ConnectionStats object.
 * It passes it into the MessageWriter constructor.
 * Both objects reference the ConnectionStats object, and keep it up to date.
 * 
 * This class doesn't have a constructor because it doesn't need one.
 * Java will initialize all the integers to 0 when ManagedConnection makes a new ConnectionStats object.
 */
public class ConnectionStats {

    /** Number of Gnutella packets we've sent this remote computer. */
    private int _numMessagesSent;

    /** Number of Gnutella packets this remote computer has sent us. */
    private int _numMessagesReceived;

    /** Of _numMessagesReceived, we dropped this many Gnutella packets after receiving them. */
    private int _numReceivedMessagesDropped;

    /** Of _numMessagesSent, we sacrificed this many Gnutella packets before sending them. */
    private int _numSentMessagesDropped;

    /*
     * getPercentReceivedDropped() and getPercentSentDropped() report the percent of messages dropped in each direction.
     * They only use data from since the last time they were called.
     * 
     * The methods save the totals from the last time they were called in these 4 variables.
     * This lets them see how the totals have grown.
     * 
     * Locking
     * These are synchronized by this class.
     * A finer-grained scheme could be used instead.
     */

    private int _lastReceived;
    private int _lastRecvDropped;
    private int _lastSent;
    private int _lastSentDropped;

    /*
     * These variables keep the total number of Gnutella packets we've exchanged with this remote computer.
     * 
     *   _numMessagesSent     Total number of packets we've sent to this remote computer
     *   _numMessagesReceived Total number of packets this remote computer has sent us
     * 
     * Of those totals, here are the number we dropped in each direction.
     * 
     *   _numSentMessagesDropped     Of the total, we sacrificed this many to keep under a bandwidth limit
     *   _numReceivedMessagesDropped Of the total, we threw out this many because there's something wrong with them
     * 
     * The MessageQueue drops messages before sending them to keep to a prescribed bandwidth limit.
     * The SpamFilter or MessageRouter drop messages we receive if we don't like them.
     */

    /** Number of messages we sent this remote computer. */
    public int getSent() { return _numMessagesSent; }
    /** Number of messages this remote computer sent us. */
    public int getReceived() { return _numMessagesReceived; }
    /** Number of messages we meant to send this remote computer, but the MessageQueue killed them to limit bandwidth. */
    public int getSentDropped() { return _numSentMessagesDropped; }
    /** Number of messages we got from this remote computer and then the SpamFilter or MessageRouter told us to drop. */
    public int getReceivedDropped() { return _numReceivedMessagesDropped; }

    /**
     * The MessageQueue dropped another number of messages, add them to our total.
     * 
     * MessageWriter.handleWrite() and MessageWriter.send() call this.
     * In both places, they ask the MessageWriter how many packets it dropped, and then reset it to start counting up from 0 again.
     * Code passes the drop count here, where we add it to the total.
     * 
     * @param dropped The number of messages the MessageQueue dropped since the last time the MessageWriter asked about it
     */
    public void addSentDropped(int dropped) {

        // Add the given number of messages to the total
        _numSentMessagesDropped += dropped;
    }

    /**
     * Count another message we mean to send this remote computer.
     * 
     * MessageWriter.send() calls this before giving a message to the MessageQueue.
     * The MessageQueue might send the message, or it might drop it.
     */
    public void addSent() {

        // Increment our total of the number of messages we've released to send to this remote computer
        _numMessagesSent++;
    }

    /**
     * Count another message we received, didn't like, and dropped.
     * 
     * This is called 2 places.
     * Code in the MessageRouter calls this when it drops a packet.
     * ManagedConnection.handleMessageInternal() calls this when the spam filter for routing tells us to drop the packet.
     */
    public void addReceivedDropped() {

        // Increment our total of messages we've received from this remote computer, examined, and then dropped
        _numReceivedMessagesDropped++;
    }

    /**
     * Count another message the remote computer sent us.
     * 
     * ManagedConnection.processReadMessage(m) calls this when the MessageReader has given it a freshly sliced Gnutella packet.
     */
    public void addReceived() {

        // Increment our total of received messages from this remote computer
        _numMessagesReceived++;
    }

    /**
     * If the remote computer sent us 100 packets and then we dropped 20, returns 20%.
     * Uses counts since the last time you called this method.
     * 
     * @return The received dropped percent, like 20
     */
    public synchronized float getPercentReceivedDropped() {

        // Calculate the number of messages we've received, and of those, how many we dropped, since the last time code called here
        int rdiff = _numMessagesReceived        - _lastReceived;    // The number of messages we received
        int ddiff = _numReceivedMessagesDropped - _lastRecvDropped; // Of those, the number we dropped

        // If we dropped 20 packets of 100 received, it's 20%
        float percent = (rdiff == 0) ? 0.f : ((float)ddiff / (float)rdiff * 100.f);

        // Save the current values over the last values to use them next time
        _lastReceived    = _numMessagesReceived;
        _lastRecvDropped = _numReceivedMessagesDropped;

        // Return the percent we calculated
        return percent;
    }

    /**
     * If we prepared 100 packets and then sacrificed 20 to stay within a bandwidth limit, returns 20%.
     * Uses counts since the last time you called this method.
     * 
     * @return The sent dropped percent, like 20
     */
    public synchronized float getPercentSentDropped() {

        /*
         * TODO:kfaaborg The Javadoc that was here may be wrong. It says:
         * 
         * @effects Returns the percentage of messages sent on this
         *  since the last call to getPercentSentDropped that were
         *  dropped by this end of the connection.  This value may be
         *  greater than 100%, e.g., if only one message is sent but
         *  four are dropped during a given time period.
         * 
         * It looks like the value can never go above 100%, as the denominator is always the total number of messages, not just those that make it.
         */

        // Calculate the number of messages we've sent, and of those, how many we sacrificed, since the last time code called here
        int rdiff = _numMessagesSent        - _lastSent;        // The number of messages we received
        int ddiff = _numSentMessagesDropped - _lastSentDropped; // Of those, the number we sacrificed

        // If we sacrificed 20 packets of 100 we meant to send, it's 20%
        float percent = (rdiff == 0) ? 0.f : ((float)ddiff / (float)rdiff * 100.f);

        // Save the current values over the last values to use them next time
        _lastSent        = _numMessagesSent;
        _lastSentDropped = _numSentMessagesDropped;

        // Return the percent we calculated
        return percent;
    }
}
