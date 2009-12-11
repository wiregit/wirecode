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

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList.Connector;
import ca.odell.glazedlists.impl.ThreadSafeList;

import com.google.inject.Singleton;

@Singleton
public class MockUploadListManager implements UploadListManager {

    private EventList<UploadItem> uploadItems;
    private ThreadSafeList<UploadItem> threadSafeUploadItems;
    private EventList<UploadItem> swingThreadUploadItems;

    public MockUploadListManager() {
        threadSafeUploadItems = GlazedListsFactory.threadSafeList(new BasicEventList<UploadItem>());
        Connector<UploadItem> uploadConnector = GlazedLists.beanConnector(UploadItem.class);
        uploadItems = GlazedListsFactory.observableElementList(threadSafeUploadItems, uploadConnector);
                 
  
        addUpload(UploadState.UPLOADING, "Weely_Address.mp3", 1048576, 0, Category.AUDIO, "DenseHedgehog-198-123");
        addUpload(UploadState.QUEUED, "Monkey_Laughing.mov", 54000, 150, Category.VIDEO, "GreenCat-98-53");

        addUpload(UploadState.BROWSE_HOST, "FastSnail32.213", 300, 15, Category.DOCUMENT, "TriteApple-18-133");
        addUpload(UploadState.BROWSE_HOST_DONE, "FastSnail12.33", 300, 15, Category.DOCUMENT, "HappyFrog-98-123");
        
        addUpload(UploadState.LIMIT_REACHED, "Monkey_on_a_Skateboard.bmp", 5500, 15, Category.IMAGE, "EasySnake-3-123");
        
        addUpload(UploadState.PAUSED, "DewDrops.avi", 522300, 15, Category.VIDEO, "FastFlounder-8-133");
        addUpload(UploadState.REQUEST_ERROR, "Twelfth_Night.txt", 3000, 15, Category.DOCUMENT, "FatRabbit-198-123");
        addUpload(UploadState.DONE, "LimeWire.exe", 5200, 15, Category.PROGRAM, "SkinnyCow-18-123");
        
    }
    
    private void addUpload(UploadState state, String fileName, long fileSize, long amtUploaded, Category category, String hostName) {
        UploadItem item = new MockUploadItem(state, fileName, fileSize, amtUploaded, category, hostName);
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
