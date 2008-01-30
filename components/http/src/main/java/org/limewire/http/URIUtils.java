package org.limewire.http;

import java.net.URISyntaxException;

import org.limewire.service.ErrorService;

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
        // TODO temporary - to get feedback about the robustness java.net.URI()
        ErrorService.error(e);
    }
}
