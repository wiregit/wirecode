package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.FileDetails;

/** 
 * Allows ContentRequests to be sent.
 * Different ContentAuthorities can send messages to different folks
 * and request permissions from different authorities.
 * 
 * For example..
 *  A IpPortContentAuthority could send messages to a single IpPort.
 *  A DhtContentAuthority could send messages to a DHT.
 */
public interface ContentAuthority {
    
    /** 
     * Initializes this authority. This is allowed to block. 
     * 
     * @throws Exception if initialization fails
     */
    public void initialize() throws Exception;
    
    public void shutdown();
    /**
     * Sets the observer that is notified of authorization replies.
     */
    public void setContentResponseObserver(ContentAuhorityResponseObserver observer);
    
    /** Sends a message through this authority. This should not block. */
    public void sendAuthorizationRequest(FileDetails details);
    /**
     * Returns the timeout in in milliseconds it makes sense to wait for
     * this authority to reply to an authorization request.
     * 
     * @return 0 if the requester should decide for himself how long to
     * wait
     */
    long getTimeout();
}
