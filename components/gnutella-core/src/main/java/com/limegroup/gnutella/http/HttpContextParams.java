package com.limegroup.gnutella.http;

import org.apache.http.protocol.HttpContext;
import org.limewire.http.HttpIOSession;

import com.limegroup.gnutella.FileDetails;

public class HttpContextParams {

    public final static String FILE_DETAILS = "org.limewire.filedetails";
    
    public final static String ALT_LOC_TRACKER = "org.limewire.altloctracker";

    public final static String LOCAL = "org.limewire.local";

    private static final String IO_SESSION = "org.limewire.iosession";

    public static FileDetails getFileDetails(final HttpContext context) {
        return (FileDetails) context.getAttribute(FILE_DETAILS);
    }
    
    public static void setFileDetails(final HttpContext context, final FileDetails fd) {
        context.setAttribute(FILE_DETAILS, fd);
    }

    public static AltLocTracker getAltLocTracker(final HttpContext context) {
        return (AltLocTracker) context.getAttribute(ALT_LOC_TRACKER);
    }

    public static void setAltLocTracker(final HttpContext context, final AltLocTracker tracker) {
        context.setAttribute(ALT_LOC_TRACKER, tracker);
    }

    public static boolean isLocal(final HttpContext context) {
        Object o = context.getAttribute(LOCAL);
        return (o != null) ? (Boolean) o : false;
    }

    public static void setLocal(final HttpContext context, final boolean local) {
        context.setAttribute(LOCAL, local);
    }

    public static void setIOSession(HttpContext context, HttpIOSession session) {
        context.setAttribute(IO_SESSION, session);
    }
    
    public static HttpIOSession getIOSession(HttpContext context) {
        return (HttpIOSession) context.getAttribute(IO_SESSION);
    }

}
