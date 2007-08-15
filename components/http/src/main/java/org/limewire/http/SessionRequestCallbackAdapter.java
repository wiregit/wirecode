package org.limewire.http;

import org.apache.http.nio.reactor.SessionRequest;
import org.apache.http.nio.reactor.SessionRequestCallback;

/**
 * The methods in this class are empty. This class exists as convenience for
 * creating listener objects.
 */
public class SessionRequestCallbackAdapter implements SessionRequestCallback {

    public void cancelled(SessionRequest request) {
    }

    public void completed(SessionRequest request) {
    }

    public void failed(SessionRequest request) {
    }

    public void timeout(SessionRequest request) {
    }

}
