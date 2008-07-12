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
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SearchServices;
import com.limegroup.gnutella.messages.QueryReply;

class CoreSearch implements Search {

    private final SearchDetails searchDetails;
    private final SearchServices searchServices;
    private final QueryReplyListenerList listenerList;
    private AtomicBoolean started = new AtomicBoolean(false);
    private volatile byte[] searchGuid;
    private volatile QueryReplyListener listener;

    @AssistedInject
    public CoreSearch(@Assisted
    SearchDetails searchDetails, SearchServices searchServices,
            QueryReplyListenerList listenerList) {
        this.searchDetails = searchDetails;
        this.searchServices = searchServices;
        this.listenerList = listenerList;
    }

    @Override
    public void start(final SearchListener searchListener) {
        if (started.getAndSet(true)) {
            throw new IllegalStateException("already started!");
        }

        searchGuid = searchServices.newQueryGUID();
        listener = new QrListener(searchListener);
        listenerList.addQueryReplyListener(searchGuid, listener);

        searchServices.query(searchGuid, searchDetails.getSearchQuery(), "",
                toMediaType(searchDetails.getSearchCategory()));
    }

    @Override
    public void stop() {
        listenerList.removeQueryReplyListener(searchGuid, listener);
    }

    private MediaType toMediaType(SearchCategory searchCategory) {
        switch (searchCategory) {
        case ALL:
            return MediaType.getAnyTypeMediaType();
        case AUDIO:
            return MediaType.getAudioMediaType();
        case DOCUMENTS:
            return MediaType.getDocumentMediaType();
        case IMAGES:
            return MediaType.getImageMediaType();
        case VIDEO:
            return MediaType.getVideoMediaType();
        default:
            throw new IllegalArgumentException(searchCategory.name());
        }
    }

    private static class QrListener implements QueryReplyListener {
        private final SearchListener searchListener;

        public QrListener(SearchListener searchListener) {
            this.searchListener = searchListener;
        }

        @Override
        public void handleQueryReply(RemoteFileDesc rfd, QueryReply queryReply,
                Set<? extends IpPort> locs) {
            searchListener.handleSearchResult(new SearchResultAdapter(rfd,
                    queryReply, locs));
        }
    }

}
