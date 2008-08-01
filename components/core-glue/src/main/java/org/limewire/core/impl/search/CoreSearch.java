package org.limewire.core.impl.search;

import java.util.Set;
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
    private volatile byte[] searchGuid;
    private volatile QrListener listener;

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
    public void start(SearchListener searchListener) {
        if (started.getAndSet(true)) {
            throw new IllegalStateException("already started!");
        }

        doSearch(searchListener);
    }
    
    private void doSearch(SearchListener searchListener) {
        searchGuid = searchServices.newQueryGUID();
        listener = new QrListener(searchListener);
        listenerList.addQueryReplyListener(searchGuid, listener);

        searchServices.query(searchGuid, searchDetails.getSearchQuery(), "",
                MediaTypeConverter.toMediaType(searchDetails.getSearchCategory()));
    }
    
    @Override
    public void repeat() {
        if(!started.get()) {
            throw new IllegalStateException("must start!");
        }
        
        stop();
        doSearch(listener.searchListener);
    }

    @Override
    public void stop() {
        if(!started.get()) {
            throw new IllegalStateException("must start!");
        }
        
        listenerList.removeQueryReplyListener(searchGuid, listener);
        searchServices.stopQuery(new GUID(searchGuid));
    }

    private static class QrListener implements QueryReplyListener {
        private final SearchListener searchListener;

        public QrListener(SearchListener searchListener) {
            this.searchListener = searchListener;
        }

        @Override
        public void handleQueryReply(RemoteFileDesc rfd, QueryReply queryReply,
                Set<? extends IpPort> locs) {
            searchListener.handleSearchResult(new RemoteFileDescAdapter(rfd,
                    queryReply, locs));
        }
    }

    public GUID getQueryGuid() {
        return new GUID(searchGuid);
    }

}
