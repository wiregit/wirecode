package org.limewire.http;

import java.net.URISyntaxException;

import org.limewire.service.ErrorService;

public class URIUtils {

    public static void error(URISyntaxException e) {
        // TODO temporary - to get feedback about the robustness java.net.URI()
        ErrorService.error(e);
    }
}
