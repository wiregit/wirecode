package org.limewire.core.impl.library;

import java.util.Collection;
import java.util.Iterator;

import org.limewire.core.api.library.RemoteLibrary;
import org.limewire.core.api.library.RemoteLibraryEvent;
import org.limewire.core.api.library.RemoteLibraryState;
import org.limewire.core.api.search.SearchResult;
import org.limewire.listener.EventListener;

public class SearchResultListAdapter implements RemoteLibrary {

    @Override
    public void addNewResult(SearchResult file) {
    }

    @Override
    public void setNewResults(Collection<SearchResult> files) {
    }

    @Override
    public int size() {
        return 0;
    }
    
    @Override
    public void clear() {
    }

    @Override
    public Iterator<SearchResult> iterator() {
        return null;
    }

    @Override
    public RemoteLibraryState getState() {
        return null;
    }

    @Override
    public void addListener(EventListener<RemoteLibraryEvent> listener) {
    }

    @Override
    public boolean removeListener(EventListener<RemoteLibraryEvent> listener) {
        return false;
    }


}
