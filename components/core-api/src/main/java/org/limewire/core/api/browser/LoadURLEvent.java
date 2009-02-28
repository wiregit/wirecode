package org.limewire.core.api.browser;

import java.net.URI;

import org.limewire.listener.DefaultDataEvent;

public class LoadURLEvent extends DefaultDataEvent<URI>{
    public LoadURLEvent(URI uri) {
        super(uri);
    }
}
