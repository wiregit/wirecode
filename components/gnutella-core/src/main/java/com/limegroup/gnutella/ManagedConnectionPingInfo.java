package com.limegroup.gnutella;

/**
 * Contains necessary ping information per managed connection (such as last GUID,
 * next accept time (for older clients), array of needed pongs, etc.  This
 * class has a one to one mapping to a ManagedConnection.)  This information is
 * used by the Message Router to route ping replies and to throttle old clients
 * pings.
 */
public class ManagedConnectionPingInfo {
    /**
     * This is the last GUID sent by the client that we are connected to.  
     * This is used when sending cached PingReplies (PONGs).
     */
    private byte[] _lastGUID = null;

    /**
     * Used for determining if a PING received from any client is 
     * processed or dropped.  (Used in throttling clients from sending
     * too many pings).
     */
    private long _clientAcceptTime = 0;

    /**
     * Throttle time for Pings from older clients. Note:
     * it is the same as PingReplyCache.CACHE_EXPIRE_TIME, but for clarity 
     * purposes, it makes sense to have a static constant here too.
     */
    private static final long PING_THROTTLE_TIME = 3000; //3 seconds

    /**
     * Array of needed pongs where the index to the array is the Pongs 
     * (indexed by number of hops) that we want to return to the client
     */
    private int[] _neededPongs = 
        new int[MessageRouter.MAX_TTL_FOR_CACHE_REFRESH];

    /** 
     * ttl of the last Ping Request.  This is used when returning pongs.
     */
    private int _lastTTL;

    /**
     * total number of pongs needed (for all the TTLs).
     */
    private int _totalNeeded;

    public ManagedConnectionPingInfo() {
        _clientAcceptTime = System.currentTimeMillis() + PING_THROTTLE_TIME;

        resetNeeded();
        _lastTTL = 0;
        _totalNeeded = 0;
    }

    public void setLastGUID(byte[] guid) {
        this._lastGUID = guid;
    }

    /**
     * returns the last GUID sent by the client. 
     */
    public byte[] getLastGUID() {
        return _lastGUID;
    }

    public int getLastTTL() {
        return _lastTTL;
    }

    /**
     * Sets the array of needed pongs for the new ping request received.  First,
     * it, resets the old values to be 0, since not all pongs might have been
     * received.
     */
    public void setNeededPingReplies(int ttl) {
        //first, reset needed array
        resetNeeded();

        if (ttl > _neededPongs.length)
            ttl = _neededPongs.length;

        for (int i = 0; i < ttl; i++) 
            _neededPongs[i] = MessageRouter.MAX_PONGS_TO_RETURN / ttl;

        _lastTTL = ttl;
    }

    /**
     * Returns the array of needed ping replies.
     */
    public int[] getNeededPingReplies() {
        return _neededPongs;
    }

    /**
     * Resets the array of needed pongs.
     */
    private void resetNeeded() {
        for (int i = 0; i < _neededPongs.length; i++)
            _neededPongs[i] = 0;
    }

    /**
     * Calculates and returns the total number of ping replies needed
     * (regardless of TTL)
     */
    public int getTotalNeeded() {
        int count = 0;

        //only need to check up until the ttl of the last Ping Request.
        for (int i = 0; i < _lastTTL; i++) {
            count += _neededPongs[i];
        }

        _totalNeeded = count;
        return _totalNeeded;
    }

    /**
     * Determines whether to throttle (i.e., drop) a ping request.  Basically,
     * this method checks to make sure the ping received is greater than
     * THROTTLE_TIME.  If so, then it's okay to process this ping, otherwise
     * throttle the PingRequest.  If the ping is to be processed, then reset
     * the accept time for the next ping from this client.
     */
    public boolean throttlePing() {
        if (System.currentTimeMillis() > _clientAcceptTime) {
            _clientAcceptTime = System.currentTimeMillis() + 
                PING_THROTTLE_TIME;
            return false;
        }
        else {
            return true;
        }
    }

}
