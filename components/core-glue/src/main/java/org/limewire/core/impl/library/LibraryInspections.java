package org.limewire.core.impl.library;

import java.util.Random;

import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.inject.LazySingleton;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectionPoint;
import org.limewire.inspection.SwingInspectable;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;

/**
 * Set of inspections for misc library stats. 
 */
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
    
    private class GetRandomPublicFileBytesInspectable extends SwingInspectable {
        @Override
        public Object inspectOnEDT() {
            if (sharedFileListManager.getModel().size() <= 0) {
                return -1;
            }
            
            SharedFileList publicList = sharedFileListManager.getModel().get(0);
            EventList<LocalFileItem> publicFileList = publicList.getSwingModel();
            
            if (publicFileList.size() <= 0) {
                return -1;
            }
            
            double random = new Random(System.currentTimeMillis()).nextDouble();
            int randomFileIndex = (int) (random*publicFileList.size());
            return publicFileList.get(randomFileIndex).getSize();
        }
    }
    
    private class GetRandomFileBytesInspectable extends SwingInspectable {
        @Override
        public Object inspectOnEDT() {
            if (sharedFileListManager.getModel().size() <= 0) {
                return -1;
            }
            
            LibraryFileList libraryList = libraryManager.getLibraryManagedList();
            EventList<LocalFileItem> libraryFileList = libraryList.getSwingModel();
            
            if (libraryFileList.size() <= 0) {
                return -1;
            }
            
            double random = new Random(System.currentTimeMillis()).nextDouble();
            int randomFileIndex = (int) (random*libraryFileList.size());
            return libraryFileList.get(randomFileIndex).getSize();
        }
    }
}
