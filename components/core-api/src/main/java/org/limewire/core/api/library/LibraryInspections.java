package org.limewire.core.api.library;

import java.util.Random;

import org.limewire.inject.LazySingleton;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

@LazySingleton
public class LibraryInspections {

    private final SharedFileListManager sharedFileListManager;
    private final LibraryManager libraryManager;

    @Inject
    public LibraryInspections(SharedFileListManager sharedFileListManager, LibraryManager libraryManager) {
        this.sharedFileListManager = sharedFileListManager;
        this.libraryManager = libraryManager;
    }

    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint(value = "random public file bytes", category = DataCategory.USAGE)
        private final Inspectable randomPublicFileBytes = new GetRandomPublicFileBytesInspectable();
        
        @InspectionPoint(value = "random file bytes", category = DataCategory.USAGE)
        private final Inspectable randomFileBytes = new GetRandomFileBytesInspectable();
    }
    
    private class GetRandomPublicFileBytesInspectable implements Inspectable {
        @Override
        public Object inspect() {
            SharedFileList publicList = sharedFileListManager.getModel().get(0);
            EventList<LocalFileItem> publicFileList = publicList.getSwingModel();
            
            double random = new Random(System.currentTimeMillis()).nextDouble();
            int randomFileIndex = (int) (random*publicFileList.size());
            return publicFileList.get(randomFileIndex).getSize();
        }
    }
    
    private class GetRandomFileBytesInspectable implements Inspectable {
        @Override
        public Object inspect() {
            LibraryFileList libraryList = libraryManager.getLibraryManagedList();
            EventList<LocalFileItem> libraryFileList = libraryList.getSwingModel();
            
            double random = new Random(System.currentTimeMillis()).nextDouble();
            int randomFileIndex = (int) (random*libraryFileList.size());
            return libraryFileList.get(randomFileIndex).getSize();
        }
    }
}
