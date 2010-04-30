package org.limewire.core.impl.search.torrentweb;

import java.net.URI;

public interface TorrentUriPrioritizerFactory {
    TorrentUriPrioritizer create(String query, URI referrer);
}
