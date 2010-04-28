package org.limewire.core.impl.search;

import org.limewire.core.api.search.SearchListener;

public interface TorrentWebSearchFactory {

    TorrentWebSearch create(String query, SearchListener searchListener);
}
