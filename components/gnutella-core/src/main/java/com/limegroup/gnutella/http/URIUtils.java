package com.limegroup.gnutella.http;

import java.net.URISyntaxException;

import org.limewire.service.ErrorService;

import com.limegroup.gnutella.settings.BugSettings;

/**
 * Utilities for URIs
 */
public class URIUtils {

    /**
     * a temporary method to allow the tracking of URI's
     * that cannot be constructed via java.net.URI.
     * 
     * Sends feedback via the ErrorService.
     * 
     * @param e
     */
    public static void error(URISyntaxException e) {
        if (BugSettings.SEND_URI_BUGS.getValue())
            ErrorService.error(e);
    }
}
