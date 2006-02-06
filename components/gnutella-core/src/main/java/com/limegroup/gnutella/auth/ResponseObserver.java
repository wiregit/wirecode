package com.limegroup.gnutella.auth;

import com.limegroup.gnutella.URN;

/** An observer for URNs. */
public interface ResponseObserver {
    public boolean handleResponse(URN urn, Response response);
}
