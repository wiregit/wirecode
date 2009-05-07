package org.limewire.core.impl.library;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.FileCollectionManager;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.FileViewManager;
import com.limegroup.gnutella.library.SharedFileCollection;

/**
 * Implementation of the FriendFileList interface, used to keep track of what
 * files are shared with a specific friend.
 */
// TODO: This really should only deal with collections, but
//       because the UI still wants to know about "shared with",
//       we have to keep track of all the views for now.
class FriendFileListImpl extends AbstractFriendFileList {
    private final FileCollectionManager collectionManager;
    private final FileViewManager viewManager;

    private volatile SharedFileCollection friendCollection;
    private volatile FileView friendView;

    private final String name;

    private volatile boolean committed = false;

    private volatile EventListener<FileViewChangeEvent> eventListener;

    private final CombinedShareList combinedShareList;

    FriendFileListImpl(CoreLocalFileItemFactory coreLocalFileItemFactory,
            FileCollectionManager collectionManager, FileViewManager viewManager, String name,
            CombinedShareList combinedShareList) {
        super(combinedShareList.createMemberList(), coreLocalFileItemFactory);
        this.viewManager = viewManager;
        this.collectionManager = collectionManager;
        this.name = name;
        this.combinedShareList = combinedShareList;
    }

    @Override
    protected SharedFileCollection getMutableCollection() {
        if(friendCollection == null) {
            friendCollection = collectionManager.getOrCreateSharedCollectionByName(name);
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
        friendView = viewManager.getFileViewForId(name);
        friendView.addFileViewListener(eventListener);
        combinedShareList.addMemberList(baseList);

        friendView.getReadLock().lock();
        try {
            // TODO: this isn't safe because adding can trigger callbacks and we're holding friendView's lock here. 
            addAllFileDescs(friendView);
        } finally {
            friendView.getReadLock().unlock();
        }
    }
}