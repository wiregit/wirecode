package org.limewire.core.impl.library;

import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileView;

public class SimpleFriendFileListImpl extends LocalFileListImpl {
   
    private final FileCollection gnutellaCollection;
    private final FileView gnutellaView;

    public SimpleFriendFileListImpl(CoreLocalFileItemFactory coreLocalFileItemFactory,
            FileCollection gnutellaFileCollection, FileView gnutellaFileView,
            CombinedShareList combinedShareList) {
        super(combinedShareList.createMemberList(), coreLocalFileItemFactory);
        this.gnutellaCollection = gnutellaFileCollection;
        this.gnutellaView = gnutellaFileView;
        this.gnutellaView.addListener(newEventListener());
        combinedShareList.addMemberList(baseList);
    }
    
    @Override
    protected FileCollection getMutableCollection() {
        return gnutellaCollection;
    }
    
    @Override
    protected FileView getFileView() {
        return gnutellaView;
    }

}