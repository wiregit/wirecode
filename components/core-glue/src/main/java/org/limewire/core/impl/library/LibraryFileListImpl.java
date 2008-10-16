/**
 * 
 */
package org.limewire.core.impl.library;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.impl.URNImpl;
import org.limewire.listener.EventListener;

import ca.odell.glazedlists.BasicEventList;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.FileManagerEvent;
import com.limegroup.gnutella.IncompleteFileDesc;

public class LibraryFileListImpl extends LocalFileListImpl implements LibraryFileList, EventListener<FileManagerEvent> {

    private final LibraryManagerImpl libraryManagerImpl;
    private final FileManager fileManager;
    
    private final ConcurrentHashMap<File, LocalFileItem> lookup;
    
    LibraryFileListImpl(LibraryManagerImpl libraryManagerImpl, FileManager fileManager) {
        super(new BasicEventList<LocalFileItem>());
        this.libraryManagerImpl = libraryManagerImpl;
        
        this.fileManager = fileManager;
        this.fileManager.addFileEventListener(this);
        
        lookup = new ConcurrentHashMap<File, LocalFileItem>();
    }
    
    @Override
    public void addFile(File file) {
        fileManager.addFile(file);
    }

    @Override
    public void removeFile(File file) {
        fileManager.removeFile(file);
    }

    @Override
    public void handleEvent(FileManagerEvent evt) {
        switch(evt.getType()) {
        case ADD_FILE:
            LocalFileItem fileItem = new CoreLocalFileItem(evt.getNewFileDesc(), this.libraryManagerImpl.detailsFactory);
            threadSafeList.add(fileItem);
            lookup.put(fileItem.getFile(), fileItem);
            break;
        case REMOVE_FILE:
            LocalFileItem removeFileItem = lookup.remove(evt.getNewFile());
            threadSafeList.remove(removeFileItem);
            break;
        case FILEMANAGER_LOAD_STARTED:
            threadSafeList.clear();
            break;
        }
    }
   
    public boolean contains(File file) {
       return fileManager.getFileDesc(file) != null;
    }
    
    public boolean contains(URN urn) {
        if(urn instanceof URNImpl) {
            URNImpl urnImpl = (URNImpl)urn; 
            FileDesc fileDesc = fileManager.getFileDesc(urnImpl.getUrn());
            return fileDesc != null && ! (fileDesc instanceof IncompleteFileDesc);
        }
        return false;
    }    
}