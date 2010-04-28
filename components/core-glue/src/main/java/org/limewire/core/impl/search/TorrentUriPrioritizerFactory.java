package org.limewire.core.impl.search;

import java.net.URI;

public interface TorrentUriPrioritizerFactory {
    TorrentUriPrioritizer create(String query, URI referrer);
}
