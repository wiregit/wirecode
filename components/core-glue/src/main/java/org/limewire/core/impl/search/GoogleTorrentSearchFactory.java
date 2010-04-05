package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchListener;

public interface GoogleTorrentSearchFactory {

    GoogleTorrentSearch create(String query, SearchListener searchListener);
}
