package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.vendor.ContentResponse;

public interface ContentManager {

    /**
     * Initializes this content manager.
     */
    public void initialize();

    /**
     * Shuts down this ContentManager.
     */
    public void shutdown();

    /** Gets the number of items in the cache. */
    public int getCacheSize();

    /** Sets the content authority. */
    public void setContentAuthority(ContentAuthority authority);

    /**
     *  Determines if we've already tried sending a request & waited the time
     *  for a response for the given URN.
     */
    public boolean isVerified(URN urn);

    /**
     * Determines if the given URN is valid.
     * 
     * @param urn
     * @param observer
     * @param timeout
     */
    public void request(URN urn, ContentResponseObserver observer, long timeout);

    /**
     * Does a request, blocking until a response is given or the request times out.
     */
    public ContentResponseData request(URN urn, long timeout);

    /**
     * Gets a response if one exists.
     */
    public ContentResponseData getResponse(URN urn);

    /**
     * Notification that a ContentResponse was given.
     */
    public void handleContentResponse(ContentResponse responseMsg);
}