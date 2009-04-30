package org.limewire.core.impl.library;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFileCollection;

/**
 * Implementation of the FriendFileList interface, used to keep track of what
 * files are shared with a specific friend.
 */
// TODO: This really should only deal with collections, but
//       because the UI still wants to know about "shared with",
//       we have to keep track of all the views for now.
class FriendFileListImpl extends AbstractFriendFileList {
    private final FileManager fileManager;

    private volatile SharedFileCollection friendCollection;
    private volatile FileView friendView;

    private final String name;

    private volatile boolean committed = false;

    private volatile EventListener<FileViewChangeEvent> eventListener;

    private final CombinedShareList combinedShareList;

    FriendFileListImpl(CoreLocalFileItemFactory coreLocalFileItemFactory, FileManager fileManager,
            String name, CombinedShareList combinedShareList) {
        super(combinedShareList.createMemberList(), coreLocalFileItemFactory);
        this.fileManager = fileManager;
        this.name = name;
        this.combinedShareList = combinedShareList;
    }

    @Override
    protected SharedFileCollection getMutableCollection() {
        if(friendCollection == null) {
            friendCollection = fileManager.getOrCreateSharedCollectionByName(name);
            friendCollection.addPersonToShareWith(name);
        }
        return friendCollection;
    }
    
    @Override
    protected FileView getFileView() {
        return friendView;
    }

    @Override
    void dispose() {
        super.dispose();
        if (committed) {
            combinedShareList.removeMemberList(baseList);
            if (friendView != null)
                friendView.removeFileViewListener(eventListener);
        }
    }

    /**
     * Commits to using this list.
     */
    void commit() {
        committed = true;
        eventListener = newEventListener();
        friendView = fileManager.getFileViewForId(name);
        friendView.addFileViewListener(eventListener);
        combinedShareList.addMemberList(baseList);

        friendView.getReadLock().lock();
        try {
            addAllFileDescs(friendView);
        } finally {
            friendView.getReadLock().unlock();
        }
    }
}