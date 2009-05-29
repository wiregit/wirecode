package org.limewire.core.impl.library;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileCollectionManager;
import com.limegroup.gnutella.library.SharedFileCollection;

@Singleton
class ShareListManagerImpl implements SharedFileListManager {
    
//    private static final Log LOG = LogFactory.getLog(ShareListManagerImpl.class);
    
    private final FileCollectionManager collectionManager;    
    private final CoreLocalFileItemFactory coreLocalFileItemFactory;
    
    // TODO: Listen to core collections & represent them.

    @Inject
    ShareListManagerImpl(FileCollectionManager collectionManager,
            CoreLocalFileItemFactory coreLocalFileItemFactory,
            LibraryManager libraryManager) {
        this.collectionManager = collectionManager;
        this.coreLocalFileItemFactory = coreLocalFileItemFactory;
    }

    @Override
    public SharedFileList createNewSharedFileList(String name) {
        SharedFileCollection collection = collectionManager.createNewCollection(name);
        SharedFileListImpl shareList = new SharedFileListImpl(coreLocalFileItemFactory, collection);
        // TODO: add to model.
        return shareList;
    }

    @Override
    public EventList<SharedFileList> getModel() {
        // TODO: return the proper list.
        return new BasicEventList<SharedFileList>();
    }
    
}
