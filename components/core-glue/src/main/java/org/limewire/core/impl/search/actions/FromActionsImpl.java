package org.limewire.core.impl.search.actions;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.actions.FromActions;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FromActionsImpl implements FromActions {
    private static final Log LOG = LogFactory.getLog(FromActionsImpl.class);

    private final RemoteLibraryManager remoteLibraryManager;

    @Inject
    public FromActionsImpl(RemoteLibraryManager remoteLibraryManager) {
        this.remoteLibraryManager = remoteLibraryManager;
    }

    @Override
    public void chatWith(RemoteHost person) {
        LOG.debugf("chatWith: {0}", person.getRenderName());
    }

    @Override
    public void showFilesSharedBy(RemoteHost person) {
        LOG.debugf("showFilesSharedBy: {0}", person.getRenderName());
    }

    @Override
    public void viewLibraryOf(final RemoteHost person) {
        // TODO: Make this work so that friend libraries are jumped to
        //       instead of browsed!
        LOG.debugf("viewLibraryOf: {0}", person.getRenderName());
        remoteLibraryManager.addPresenceLibrary(person.getFriendPresence());
    }
}
