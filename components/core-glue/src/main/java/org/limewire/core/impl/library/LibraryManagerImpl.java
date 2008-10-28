package org.limewire.core.impl.library;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.impl.URNImpl;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import ca.odell.glazedlists.BasicEventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.IncompleteFileDesc;
import com.limegroup.gnutella.library.ManagedFileList;
import com.limegroup.gnutella.library.ManagedListStatusEvent;

@Singleton
class LibraryManagerImpl implements LibraryManager {
    
    private final LibraryFileListImpl libraryList;
    
    @Inject
    public LibraryManagerImpl(ManagedFileList managedList, CoreLocalFileItemFactory coreLocalFileItemFactory) {
        this.libraryList = new LibraryFileListImpl(managedList, coreLocalFileItemFactory);
    }

    
    @Override
    public LibraryFileList getLibraryManagedList() {
        return libraryList;
    }

    private static class LibraryFileListImpl extends LocalFileListImpl implements LibraryFileList {
        private final ManagedFileList managedList;    
        private final ConcurrentHashMap<File, LocalFileItem> lookup;
        private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);
        private volatile LibraryState libraryState = LibraryState.LOADING;
        
        LibraryFileListImpl(ManagedFileList managedList, final CoreLocalFileItemFactory coreLocalFileItemFactory) {
            super(new BasicEventList<LocalFileItem>());
            this.managedList = managedList;
            this.lookup = new ConcurrentHashMap<File, LocalFileItem>();
            this.managedList.addFileListListener(new EventListener<FileListChangedEvent>() {
                @Override
                public void handleEvent(FileListChangedEvent evt) {
                    LocalFileItem item;
                    switch(evt.getType()) {
                    case ADDED:
                        item = coreLocalFileItemFactory.createCoreLocalFileItem(evt.getFileDesc());
                        threadSafeList.add(item);
                        lookup.put(item.getFile(), item);
                        break;
                    case REMOVED:
                        item = lookup.remove(evt.getFileDesc());
                        threadSafeList.remove(item);
                        break;
                    case CHANGED:
                        item = lookup.remove(evt.getOldValue());
                        threadSafeList.remove(item);            
                        item = coreLocalFileItemFactory.createCoreLocalFileItem(evt.getFileDesc());
                        threadSafeList.add(item);
                        lookup.put(item.getFile(), item);
                        break;
                    }
                }
            });
            this.managedList.addManagedListStatusListener(new EventListener<ManagedListStatusEvent>() {
                @Override
                public void handleEvent(ManagedListStatusEvent event) {
                    LibraryState oldState = libraryState;
                    switch(event.getType()) {
                    case LOAD_STARTED:
                        threadSafeList.clear();
                        lookup.clear();
                        libraryState = LibraryState.LOADING;
                        break;
                    case LOAD_COMPLETE:
                        libraryState = LibraryState.LOADED;
                        break;
                    }
                    changeSupport.firePropertyChange("state", oldState, libraryState);
                }
            });
        }
        
        @Override
        public void addFile(File file) {
            managedList.add(file);
        }

        @Override
        public void removeFile(File file) {
            managedList.remove(file);
        }
       
        public boolean contains(File file) {
           return managedList.contains(file);
        }
        
        public boolean contains(URN urn) {
            if(urn instanceof URNImpl) {
                URNImpl urnImpl = (URNImpl)urn; 
                FileDesc fileDesc = managedList.getFileDesc(urnImpl.getUrn());
                return fileDesc != null && ! (fileDesc instanceof IncompleteFileDesc);
            }
            return false;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.addPropertyChangeListener(listener);
        }

        @Override
        public LibraryState getState() {
            return libraryState;
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            changeSupport.removePropertyChangeListener(listener);
        }    
        
        
    }
    

}
