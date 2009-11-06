package org.limewire.core.impl.library;


import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectableContainer;
import org.limewire.inspection.InspectableForSize;
import org.limewire.inspection.InspectionPoint;

import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class LibraryManagerImpl implements LibraryManager {
    
    @InspectableForSize(value = "number of files in library", category = DataCategory.USAGE)
    private final LibraryFileListImpl libraryList;
    private final LibraryData libraryData;
    
    @SuppressWarnings("unused")
    @InspectableContainer
    private class LazyInspectableContainer {
        @InspectionPoint(value = "has audio in library", category = DataCategory.USAGE)
        private final Inspectable audioInLibrary = new CategoryMatcherInspectable(Category.AUDIO);
        @InspectionPoint(value = "has video in library", category = DataCategory.USAGE)
        private final Inspectable videoInLibrary = new CategoryMatcherInspectable(Category.VIDEO);
        @InspectionPoint(value = "has documents in library", category = DataCategory.USAGE)
        private final Inspectable documentsInLibrary = new CategoryMatcherInspectable(Category.DOCUMENT);
        @InspectionPoint(value = "has images in library", category = DataCategory.USAGE)
        private final Inspectable imagesInLibrary = new CategoryMatcherInspectable(Category.IMAGE);
        @InspectionPoint(value = "has others in library", category = DataCategory.USAGE)
        private final Inspectable otherInLibrary = new CategoryMatcherInspectable(Category.OTHER);
        @InspectionPoint(value = "has programs in library", category = DataCategory.USAGE)
        private final Inspectable programsInLibrary = new CategoryMatcherInspectable(Category.PROGRAM);
        
        
        private class CategoryMatcherInspectable implements Inspectable {
            private Category category;
            public CategoryMatcherInspectable(Category category){
             this.category = category;   
            }
            @Override
            public Object inspect() {
                for (FileItem item : libraryList.getModel()){
                    if(item.getCategory() == category){
                        return true;
                    }
                }
                return false;
            }            
        }
    }
    
    @Inject
    public LibraryManagerImpl(LibraryFileListImpl libraryFileListImpl, LibraryData libraryData) {
        this.libraryList = libraryFileListImpl;
        this.libraryData = libraryData;
    }
    
    @Override
    public LibraryFileList getLibraryManagedList() {
        return libraryList;
    }
    
    public LibraryData getLibraryData() {
        return libraryData;
    }

    @Override
    public ListEventPublisher getLibraryListEventPublisher() {
        return libraryList.getModel().getPublisher();
    }

    @Override
    public ReadWriteLock getReadWriteLock() {
        return libraryList.getModel().getReadWriteLock();
    }
}
