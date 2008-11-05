/**
 * 
 */
package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.impl.URNImpl;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileList;
import com.limegroup.gnutella.library.FileListChangedEvent;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;

abstract class LocalFileListImpl implements LocalFileList {
    protected final EventList<LocalFileItem> baseList;
    protected final TransformedList<LocalFileItem, LocalFileItem> threadSafeList;
    protected final TransformedList<LocalFileItem, LocalFileItem> readOnlyList;
    protected volatile TransformedList<LocalFileItem, LocalFileItem> swingEventList;
    

    private final Map<File, FileItem> lookup;
    private final CoreLocalFileItemFactory fileItemFactory;
    
    LocalFileListImpl(EventList<LocalFileItem> eventList, CoreLocalFileItemFactory fileItemFactory) {
        this.baseList = eventList;
        this.threadSafeList = GlazedListsFactory.threadSafeList(eventList);
        this.readOnlyList = GlazedListsFactory.readOnlyList(threadSafeList);
        this.fileItemFactory = fileItemFactory;
        this.lookup = new ConcurrentHashMap<File, FileItem>();

    }
    
    /** Returns the FileList this should act on. */
    protected abstract FileList getCoreFileList();
    
    @Override
    public void addFile(File file) {
        getCoreFileList().add(file);
    }

    @Override
    public void removeFile(File file) {
        getCoreFileList().remove(file);
    }
    
    @Override
    public void addFolder(File folder) {
        getCoreFileList().addFolder(folder);
    }

    @Override
    public boolean contains(File file) {
        return getCoreFileList().contains(file);
    }
    
    @Override
    public boolean contains(URN urn) {
        if(urn instanceof URNImpl) {
            return containsCoreUrn(((URNImpl)urn).getUrn());
        } else {
            return false;
        }
    }
    
    protected boolean containsCoreUrn(com.limegroup.gnutella.URN urn) {
        return !getCoreFileList().getFileDescsMatching(urn).isEmpty();
    }

    @Override
    public EventList<LocalFileItem> getModel() {
        return readOnlyList;
    }
    
    @Override
    public EventList<LocalFileItem> getSwingModel() {
        assert EventQueue.isDispatchThread();
        if(swingEventList == null) {
            swingEventList =  GlazedListsFactory.swingThreadProxyEventList(readOnlyList);
        }
        return swingEventList;
    }
    
    void dispose() {
        if(swingEventList != null) {
            swingEventList.dispose();
        }
        threadSafeList.dispose();
        readOnlyList.dispose();
    }
    
    @Override
    public int size() {
        return threadSafeList.size();
    }
    
    protected void addFileDesc(FileDesc fd) {
        LocalFileItem newItem = fileItemFactory.createCoreLocalFileItem(fd);
        lookup.put(fd.getFile(), newItem);
        threadSafeList.add(newItem);
    }
    
    protected void changeFileDesc(FileDesc old, FileDesc now) {
        removeFileDesc(old);
        addFileDesc(now);
    }
    
    protected void removeFileDesc(FileDesc fd) {
        FileItem old = lookup.remove(fd.getFile());
        threadSafeList.remove(old);
    }
    
    protected void clearFileDescs() {
        lookup.clear();
        threadSafeList.clear();
    }
   
    /** Constructs a new EventListener for list change events. */
    protected EventListener<FileListChangedEvent> newEventListener() {
        return new EventListener<FileListChangedEvent>() {
            @Override
            public void handleEvent(FileListChangedEvent event) {
                switch(event.getType()) {
                case ADDED:
                    addFileDesc(event.getFileDesc());
                    break;
                case CHANGED:
                    changeFileDesc(event.getOldValue(), event.getFileDesc());
                    break;
                case REMOVED:
                    removeFileDesc(event.getFileDesc());
                    break;
                case CLEAR:
                    clearFileDescs();
                    break;                
                }
            }
        };
    }
}