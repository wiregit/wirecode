package com.limegroup.gnutella.http;

import org.apache.http.protocol.HttpContext;

import com.limegroup.gnutella.FileDetails;

public class HttpContextParams {

    public final static String FILE_DETAILS = "org.limewire.filedetails";
    
    public final static String ALT_LOC_TRACKER = "org.limewire.altloctracker";
    
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
    
}
