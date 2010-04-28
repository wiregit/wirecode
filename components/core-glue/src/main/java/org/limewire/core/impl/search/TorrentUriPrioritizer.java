package org.limewire.core.impl.search;

import java.net.URI;
import java.util.List;

public interface TorrentUriPrioritizer {
    List<URI> prioritize(List<URI> candidates);
    void setIsTorrent(URI uri, boolean isTorrent);
}
