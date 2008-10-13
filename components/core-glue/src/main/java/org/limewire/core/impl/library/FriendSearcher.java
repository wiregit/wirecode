package org.limewire.core.impl.library;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.impl.search.FriendSearchListener;
import org.limewire.core.settings.SearchSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendSearcher {
    private final FriendLibraries libraries;

    @Inject
    FriendSearcher(FriendLibraries libraries) {
        this.libraries = libraries;
    }
    
    public void doSearch(SearchDetails searchDetails, FriendSearchListener listener) {
        if(SearchSettings.SEARCH_FRIENDS_LIBRARIES.getValue()) {
            Set<RemoteFileItem> results = new HashSet<RemoteFileItem>();
            StringTokenizer st = new StringTokenizer(searchDetails.getSearchQuery());
            while(st.hasMoreElements()) {
                Iterator<RemoteFileItem> resultsForWord = libraries.iterator(st.nextToken(), searchDetails.getSearchCategory());
                while (resultsForWord.hasNext()) {
                    results.add(resultsForWord.next());
                }
            }
            listener.handleFriendResults(results.iterator());
        }
    }
}
