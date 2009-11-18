package org.limewire.core.impl.upload;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;

import com.google.inject.Singleton;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList.Connector;
import ca.odell.glazedlists.impl.ThreadSafeList;

@Singleton
public class MockUploadListManager implements UploadListManager {

    private EventList<UploadItem> uploadItems;
    private ThreadSafeList<UploadItem> threadSafeUploadItems;
    private EventList<UploadItem> swingThreadUploadItems;

    public MockUploadListManager() {
        threadSafeUploadItems = GlazedListsFactory.threadSafeList(new BasicEventList<UploadItem>());
        Connector<UploadItem> uploadConnector = GlazedLists.beanConnector(UploadItem.class);
        uploadItems = GlazedListsFactory.observableElementList(threadSafeUploadItems, uploadConnector);
        
        addUpload(UploadState.DONE, "File.mp3", 30000, 15000, Category.AUDIO);
        addUpload(UploadState.QUEUED, "File.avi", 3000, 150, Category.VIDEO);
        addUpload(UploadState.UPLOADING, "File2.mp3", 1048576, 0, Category.AUDIO);
        addUpload(UploadState.DONE, "File3.exe", 300, 150, Category.PROGRAM);
        addUpload(UploadState.FILE_ERROR, "File3.doc", 300, 15, Category.DOCUMENT);
        addUpload(UploadState.BROWSE_HOST, "string", 300, 15, Category.DOCUMENT);
        
    }
    
    private void addUpload(UploadState state, String fileName, long fileSize, long amtUploaded, Category category) {
        UploadItem item = new MockUploadItem(state, fileName, fileSize, amtUploaded, category);
        threadSafeUploadItems.add(item);
        item.addPropertyChangeListener(new UploadPropertyListener(item));
    }

    @Override
    public EventList<UploadItem> getSwingThreadSafeUploads() {
        if (swingThreadUploadItems == null) {
            swingThreadUploadItems = GlazedListsFactory.swingThreadProxyEventList(uploadItems);
        }
        return swingThreadUploadItems;
    }

    @Override
    public List<UploadItem> getUploadItems() {
        return uploadItems;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }
    
    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }
    
    @Override
    public void updateUploadsCompleted() {
    }
    
    private class UploadPropertyListener implements PropertyChangeListener {
        private UploadItem item;

        public UploadPropertyListener(UploadItem item){
            this.item = item;
        }
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (item.getState() == UploadState.CANCELED) {
                remove(item);
            }
        }
    }
    
    @Override
    public void clearFinished() {
        List<UploadItem> finishedItems = new ArrayList<UploadItem>();
        threadSafeUploadItems.getReadWriteLock().writeLock().lock();
        try {
            for(UploadItem item : threadSafeUploadItems){
                UploadState state = item.getState();
                if (state.isFinished() || state.isError()) {
                    finishedItems.add(item);
                }
            }
            threadSafeUploadItems.removeAll(finishedItems);
        } finally {
            threadSafeUploadItems.getReadWriteLock().writeLock().unlock();
        }
    }
    
    @Override
    public void remove(UploadItem item) {
        threadSafeUploadItems.remove(item);
    }
}
