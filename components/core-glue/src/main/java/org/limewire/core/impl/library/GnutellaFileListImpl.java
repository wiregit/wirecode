package org.limewire.core.impl.library;

import org.limewire.core.api.library.GnutellaFileList;

import com.limegroup.gnutella.library.GnutellaFileCollection;

/**
 * Implementation of the GnutellaFileList interface, used to keep track of what
 * files are shared with the gnutella network.
 */
class GnutellaFileListImpl extends AbstractFriendFileList implements GnutellaFileList {
    private final com.limegroup.gnutella.library.GnutellaFileCollection shareList;

    public GnutellaFileListImpl(CoreLocalFileItemFactory coreLocalFileItemFactory,
            com.limegroup.gnutella.library.GnutellaFileCollection shareList,
            CombinedShareList combinedShareList) {
        super(combinedShareList.createMemberList(), coreLocalFileItemFactory);
        this.shareList = shareList;
        this.shareList.addFileListListener(newEventListener());
        combinedShareList.addMemberList(baseList);
    }

    @Override
    protected GnutellaFileCollection getCoreFileList() {
        return shareList;
    }

    @Override
    public void removeDocuments() {
        getCoreFileList().removeDocuments();
    }
}