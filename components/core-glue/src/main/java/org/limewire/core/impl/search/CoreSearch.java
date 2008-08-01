package org.limewire.core.impl.search;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchListener;
import org.limewire.io.IpPort;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.QueryReply;

public class CoreSearch implements Search {

    private final SearchDetails searchDetails;
    private final SearchServices searchServices;
    private final QueryReplyListenerList listenerList;
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final CopyOnWriteArrayList<SearchListener> searchListeners = new CopyOnWriteArrayList<SearchListener>();
    private final QrListener qrListener = new QrListener();
    
    private volatile byte[] searchGuid;

    @AssistedInject
    public CoreSearch(@Assisted
    SearchDetails searchDetails, SearchServices searchServices,
            QueryReplyListenerList listenerList) {
        this.searchDetails = searchDetails;
        this.searchServices = searchServices;
        this.listenerList = listenerList;
    }
    
    @Override
    public SearchCategory getCategory() {
        return searchDetails.getSearchCategory();
    }
    
    @Override
    public void addSearchListener(SearchListener searchListener) {
        searchListeners.add(searchListener);
    }
    
    @Override
    public void removeSearchListener(SearchListener searchListener) {
        searchListeners.remove(searchListener);
    }

    @Override
    public void start() {
        if (started.getAndSet(true)) {
            throw new IllegalStateException("already started!");
        }
        
        for(SearchListener listener : searchListeners) {
            listener.searchStarted();
        }

        doSearch();
    }
    
    private void doSearch() {
        searchGuid = searchServices.newQueryGUID();
        listenerList.addQueryReplyListener(searchGuid, qrListener);

        searchServices.query(searchGuid, searchDetails.getSearchQuery(), "",
                MediaTypeConverter.toMediaType(searchDetails.getSearchCategory()));
    }
    
    @Override
    public void repeat() {
        if(!started.get()) {
            throw new IllegalStateException("must start!");
        }
        
        stop();
        
        for(SearchListener listener : searchListeners) {
            listener.searchStarted();
        }
        
        doSearch();
    }

    @Override
    public void stop() {
        if(!started.get()) {
            throw new IllegalStateException("must start!");
        }
        
        listenerList.removeQueryReplyListener(searchGuid, qrListener);
        searchServices.stopQuery(new GUID(searchGuid));
        
        for(SearchListener listener : searchListeners) {
            listener.searchStopped();
        }
    }


    public GUID getQueryGuid() {
        return new GUID(searchGuid);
    }

    private class QrListener implements QueryReplyListener {
        @Override
        public void handleQueryReply(RemoteFileDesc rfd, QueryReply queryReply,
                Set<? extends IpPort> locs) {
            for (SearchListener listener : searchListeners) {
                listener.handleSearchResult(new RemoteFileDescAdapter(rfd, queryReply, locs));
            }
        }
    }

}
