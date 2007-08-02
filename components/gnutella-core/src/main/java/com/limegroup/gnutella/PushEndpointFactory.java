package com.limegroup.gnutella;

public interface PushEndpointFactory {

    /** Gets the endpoint for the self. */
    public PushEndpoint createForSelf();

}