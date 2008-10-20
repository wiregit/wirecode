package org.limewire.core.impl.spam;

import java.util.List;

import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.core.impl.search.RemoteFileDescAdapter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;

@Singleton
public class SpamManagerImpl implements SpamManager {

    private final com.limegroup.gnutella.spam.SpamManager spamManager;

    @Inject
    public SpamManagerImpl(com.limegroup.gnutella.spam.SpamManager spamManager) {
        this.spamManager = spamManager;
    }

    @Override
    public void clearFilterData() {
        spamManager.clearFilterData();
    }

    @Override
    public void handleUserMarkedGood(List<SearchResult> searchResults) {
        RemoteFileDesc[] remoteFileDescs = buildArray(searchResults);
        spamManager.handleUserMarkedGood(remoteFileDescs);
    }

    @Override
    public void handleUserMarkedSpam(List<SearchResult> searchResults) {
        RemoteFileDesc[] remoteFileDescs = buildArray(searchResults);
        spamManager.handleUserMarkedSpam(remoteFileDescs);
    }

    private RemoteFileDesc[] buildArray(List<SearchResult> searchResults) {
        RemoteFileDesc[] remoteFileDescs = new RemoteFileDesc[searchResults.size()];
        int index = 0;
        for (SearchResult searchResult : searchResults) {
            RemoteFileDescAdapter remoteFileDescAdapter = (RemoteFileDescAdapter) searchResult;
            RemoteFileDesc remoteFileDesc = remoteFileDescAdapter.getRfd();
            remoteFileDescs[index++] = remoteFileDesc;
        }
        return remoteFileDescs;
    }

}
