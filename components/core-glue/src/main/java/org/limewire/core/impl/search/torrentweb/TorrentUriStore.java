package org.limewire.core.impl.search.torrentweb;

import java.net.URI;
import java.util.Set;

public interface TorrentUriStore {
    
    boolean isTorrentUri(URI uri);
    boolean isNotTorrentUri(URI uri);
    Set<URI>  getTorrentUrisForHost(String host);
    void setIsTorrentUri(URI uri, boolean isTorrent);
    void addCanonicalTorrentUris(String host, URI uri);
    
}
