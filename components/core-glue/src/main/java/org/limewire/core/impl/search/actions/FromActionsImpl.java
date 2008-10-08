package org.limewire.core.impl.search.actions;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.actions.FromActions;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileList;
import com.limegroup.gnutella.FileManager;

@Singleton
public class FromActionsImpl implements FromActions {
    private static final Log LOG = LogFactory.getLog(FromActionsImpl.class);

    private final RemoteLibraryManager remoteLibraryManager;

    private final FileManager fileManager;

    @Inject
    public FromActionsImpl(RemoteLibraryManager remoteLibraryManager, FileManager fileManager) {
        this.remoteLibraryManager = remoteLibraryManager;
        this.fileManager = fileManager;
    }

    @Override
    public void chatWith(RemoteHost person) {
        LOG.debugf("chatWith: {0}", person.getRenderName());
    }

    @Override
    public void showFilesSharedBy(RemoteHost person) {
        LOG.debugf("showFilesSharedBy: {0}", person.getRenderName());
        FileList fileList = fileManager.getFriendFileList(person.getFriendPresence().getFriend()
                .getId());
        for (FileDesc fileDesc : fileList.getAllFileDescs()) {
            System.out.println(fileDesc.getFileName());
        }

    }

    @Override
    public void viewLibraryOf(final RemoteHost person) {
        // TODO: Make this work so that friend libraries are jumped to
        // instead of browsed!
        LOG.debugf("viewLibraryOf: {0}", person.getRenderName());
        remoteLibraryManager.addPresenceLibrary(person.getFriendPresence());
    }

    @Override
    public int getNumberOfSharedFiles(RemoteHost person) {
    //TODO because of this method might want to rename the fromActions classes. or move this method.
        LOG.debugf("getNumberOfSharedFiles: {0}", person.getRenderName());
        FileList fileList = fileManager.getFriendFileList(person.getFriendPresence().getFriend()
                .getId());
        return fileList.size();
    }
}
