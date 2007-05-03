package com.limegroup.gnutella.http;

import org.apache.http.protocol.HttpContext;
import org.limewire.http.HttpIOReactor;
import org.limewire.http.HttpIOSession;

/**
 * Provides methods to access or modify objects stored in an {@link HttpContext}.
 */
public class HttpContextParams {

    /** Key for the local flag. */
    public final static String LOCAL = "org.limewire.local";

    /** Key for the subsequent request flag. */
    public final static String SUBSEQUENT_REQUEST = "org.limewire.subsequentRequest";

    public static boolean isSubsequentRequest(final HttpContext context) {
        Object o = context.getAttribute(SUBSEQUENT_REQUEST);
        return (o != null) ? (Boolean) o : false;
    }

    public static void setSubsequentRequest(final HttpContext context, final boolean local) {
        context.setAttribute(SUBSEQUENT_REQUEST, local);
    }

    public static boolean isLocal(final HttpContext context) {
        Object o = context.getAttribute(LOCAL);
        return (o != null) ? (Boolean) o : false;
    }

    public static void setLocal(final HttpContext context, final boolean local) {
        context.setAttribute(LOCAL, local);
    }

    public static void setIOSession(HttpContext context, HttpIOSession session) {
        context.setAttribute(HttpIOReactor.IO_SESSION_KEY, session);
    }
    
    public static HttpIOSession getIOSession(HttpContext context) {
        return (HttpIOSession) context.getAttribute(HttpIOReactor.IO_SESSION_KEY);
    }

}
