package org.limewire.core.impl.library;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFileCollection;

/**
 * Implementation of the FriendFileList interface, used to keep track of what
 * files are shared with a specific friend.
 */
class FriendFileListImpl extends AbstractFriendFileList {
    private final FileManager fileManager;

    private SharedFileCollection friendFileList;

    private final String name;

    private volatile boolean committed = false;

    private volatile EventListener<FileListChangedEvent> eventListener;

    private final CombinedShareList combinedShareList;

    FriendFileListImpl(CoreLocalFileItemFactory coreLocalFileItemFactory, FileManager fileManager,
            String name, CombinedShareList combinedShareList) {
        super(combinedShareList.createMemberList(), coreLocalFileItemFactory);
        this.fileManager = fileManager;
        this.name = name;
        this.combinedShareList = combinedShareList;
    }

    @Override
    protected SharedFileCollection getCoreFileList() {
        return friendFileList;
    }

    @Override
    void dispose() {
        super.dispose();
        if (committed) {
            combinedShareList.removeMemberList(baseList);
            if (friendFileList != null)
                friendFileList.removeFileListListener(eventListener);
        }
    }

    /**
     * Commits to using this list.
     */
    void commit() {
        committed = true;
        eventListener = newEventListener();
        friendFileList = fileManager.getOrCreateFriendFileList(name);
        friendFileList.addFileListListener(eventListener);
        combinedShareList.addMemberList(baseList);

        com.limegroup.gnutella.library.FileCollection fileList = friendFileList;

        fileList.getReadLock().lock();
        try {
            addAllFileDescs(fileList);
        } finally {
            fileList.getReadLock().unlock();
        }
    }
}