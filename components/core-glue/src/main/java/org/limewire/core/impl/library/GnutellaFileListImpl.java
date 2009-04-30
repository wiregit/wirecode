package org.limewire.core.impl.library;

import org.limewire.core.api.library.GnutellaFileList;

import com.limegroup.gnutella.library.FileView;
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
        this.shareList.addFileViewListener(newEventListener());
        combinedShareList.addMemberList(baseList);
    }

    @Override
    protected GnutellaFileCollection getMutableCollection() {
        return shareList;
    }
    
    @Override
    protected FileView getFileView() {
        return shareList;
    }

    @Override
    public void removeDocuments() {
        getMutableCollection().removeDocuments();
    }
}