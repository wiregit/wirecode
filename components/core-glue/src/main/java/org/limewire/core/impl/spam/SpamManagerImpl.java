package org.limewire.core.impl.spam;

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
    public void handleUserMarkedGood(SearchResult searchResult) {
        RemoteFileDescAdapter remoteFileDescAdapter = (RemoteFileDescAdapter) searchResult;
        RemoteFileDesc remoteFileDesc = remoteFileDescAdapter.getRfd();
        spamManager.handleUserMarkedGood(new RemoteFileDesc[] { remoteFileDesc });
    }

    @Override
    public void handleUserMarkedSpam(SearchResult searchResult) {
        RemoteFileDescAdapter remoteFileDescAdapter = (RemoteFileDescAdapter) searchResult;
        RemoteFileDesc remoteFileDesc = remoteFileDescAdapter.getRfd();
        spamManager.handleUserMarkedSpam(new RemoteFileDesc[] { remoteFileDesc });
    }

}
