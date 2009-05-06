/**
 * 
 */
package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ListeningFutureDelegator;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.impl.URNImpl;
import org.limewire.listener.EventListener;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;

import com.limegroup.gnutella.library.FileCollection;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileView;
import com.limegroup.gnutella.library.FileViewChangeEvent;

//TODO: This really should only deal with collections, but
//because the UI still wants to know about "shared with",
//we have to keep track of all the views for now.
abstract class LocalFileListImpl implements LocalFileList {
    
    protected final EventList<LocalFileItem> baseList;
    protected final TransformedList<LocalFileItem, LocalFileItem> threadSafeList;
    protected final TransformedList<LocalFileItem, LocalFileItem> readOnlyList;
    protected volatile TransformedList<LocalFileItem, LocalFileItem> swingEventList;    

    private final CoreLocalFileItemFactory fileItemFactory;
    
    LocalFileListImpl(EventList<LocalFileItem> eventList, CoreLocalFileItemFactory fileItemFactory) {
        this.baseList = eventList;
        this.threadSafeList = GlazedListsFactory.threadSafeList(eventList);
        this.readOnlyList = GlazedListsFactory.readOnlyList(threadSafeList);
        this.fileItemFactory = fileItemFactory;

    }
    
    /** Returns the FileCollection this should mutate. */
    protected abstract FileCollection getMutableCollection();
    
    /** Returns the FileView this should view. */
    // TODO: This will disappear when the UI starts showing collections.
    protected abstract FileView getFileView();
    
    @Override
    public ListeningFuture<LocalFileItem> addFile(File file) {
        return new Wrapper((getMutableCollection().add(file)));
    }

    @Override
    public void removeFile(File file) {
        if(contains(file)) {
            getMutableCollection().remove(file);
        }
    }
    
    @Override
    public ListeningFuture<List<ListeningFuture<LocalFileItem>>> addFolder(File folder) {
        return new ListWrapper((getMutableCollection().addFolder(folder)));
    }

    @Override
    public boolean contains(File file) {
        return getFileView().contains(file);
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
        return !getFileView().getFileDescsMatching(urn).isEmpty();
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
    
    /**
     * Adds <code>fd</code> as {@link LocalFileItem} to this list. 
     */
    protected void addFileDesc(FileDesc fd) {
        threadSafeList.add(getOrCreateLocalFileItem(fd));
    }
    
    private LocalFileItem getOrCreateLocalFileItem(FileDesc fileDesc) {
        LocalFileItem item;
        Object object = fileDesc.getClientProperty(FILE_ITEM_PROPERTY);
        if(object != null) {
            item = (LocalFileItem)object;
        } else {
            item = fileItemFactory.createCoreLocalFileItem(fileDesc);
            fileDesc.putClientProperty(FILE_ITEM_PROPERTY, item);
        }
        return item;
    }
    
    /**
     * Adds all <code>fileDescs</code> as {@link LocalFileItem} to this list.
     * <p>
     * Caller is responsible for locking the iterable.
     */
    protected void addAllFileDescs(Iterable<FileDesc> fileDescs) {
        List<LocalFileItem> fileItems = new ArrayList<LocalFileItem>();
        for (FileDesc fileDesc : fileDescs) {
            System.out.println("... adding: " + fileDesc);
            fileItems.add(getOrCreateLocalFileItem(fileDesc));
        }
        threadSafeList.addAll(fileItems);
    }
    
    protected void changeFileDesc(FileDesc old, FileDesc now) {
        removeFileDesc(old);
        addFileDesc(now);
    }
    
    protected void removeFileDesc(FileDesc fd) {
        LocalFileItem item = (LocalFileItem)fd.getClientProperty(FILE_ITEM_PROPERTY);
        threadSafeList.remove(item);
    }
    
    protected void clearFileDescs() {
        threadSafeList.clear();
    }
    
    /** Notification that a collection share has changed. */
    protected abstract void collectionUpdate(FileViewChangeEvent.Type type, boolean shared);
   
    /** Constructs a new EventListener for list change events. */
    protected EventListener<FileViewChangeEvent> newEventListener() {
        return new EventListener<FileViewChangeEvent>() {
            @Override
            public void handleEvent(FileViewChangeEvent event) {
                System.out.println("got event: " + event + ", from view: " + event.getFileView());
                
                switch(event.getType()) {
                case FILE_ADDED:
                    addFileDesc(event.getFileDesc());
                    break;
                case FILE_CHANGED:
                    changeFileDesc(event.getOldValue(), event.getFileDesc());
                    break;
                case FILE_REMOVED:
                    removeFileDesc(event.getFileDesc());
                    break;
                case FILES_CLEARED:
                    clearFileDescs();
                    break;     
                case AUDIO_COLLECTION:
                case VIDEO_COLLECTION:
                case IMAGE_COLLECTION:
                    collectionUpdate(event.getType(), event.isShared());
                    break;
                }
            }
        };
    }
    
    private class ListWrapper extends ListeningFutureDelegator<List<ListeningFuture<FileDesc>>, List<ListeningFuture<LocalFileItem>>> {
        public ListWrapper(ListeningFuture<List<ListeningFuture<FileDesc>>> delegate) {
            super(delegate);
        }
        
        @Override
        protected List<ListeningFuture<LocalFileItem>> convertSource(List<ListeningFuture<FileDesc>> source) {
            List<ListeningFuture<LocalFileItem>> replaced = new ArrayList<ListeningFuture<LocalFileItem>>(source.size());
            for(ListeningFuture<FileDesc> future : source) {
                replaced.add(new Wrapper(future));
            }
            return replaced;
        }
        
        @Override
        protected List<ListeningFuture<LocalFileItem>> convertException(ExecutionException ee)
                throws ExecutionException {
            throw ee;
        }
    }
    
    private class Wrapper extends ListeningFutureDelegator<FileDesc, LocalFileItem> {
        public Wrapper(ListeningFuture<FileDesc> delegate) {
            super(delegate);
        }
        
        @Override
        protected LocalFileItem convertSource(FileDesc source) {
            return (LocalFileItem)source.getClientProperty(FILE_ITEM_PROPERTY);
        }
        
        @Override
        protected LocalFileItem convertException(ExecutionException ee) throws ExecutionException {
            throw ee;
        }
    }
    
    @Override
    public LocalFileItem getFileItem(File file) {
      FileDesc fileDesc = getFileView().getFileDesc(file);
      if(fileDesc != null) {
          return (LocalFileItem)fileDesc.getClientProperty(FILE_ITEM_PROPERTY);
      }
      return null;
    }
}