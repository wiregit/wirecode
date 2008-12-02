package org.limewire.core.impl.upload;

import java.awt.EventQueue;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.limegroup.gnutella.Uploader;

@Singleton
public class CoreUploadListManager implements UploadListener, UploadListManager{

    private final EventList<UploadItem> uploadItems;

    private EventList<UploadItem> swingThreadUploadItems;
    
    private static final int PERIOD = 1000;


    @Inject
    public CoreUploadListManager(UploadListenerList uploadListenerList, @Named("backgroundExecutor")
            ScheduledExecutorService backgroundExecutor) {

        ObservableElementList.Connector<UploadItem> uploadConnector = GlazedLists.beanConnector(UploadItem.class);
        
        uploadItems = GlazedListsFactory.threadSafeList(GlazedListsFactory.observableElementList(
                new BasicEventList<UploadItem>(),uploadConnector)) ;

        uploadListenerList.addUploadListener(this);
        
        //TODO: change backgroundExecutor to listener - currently no listener for upload progress
        //hack to force tables to update
          Runnable command = new Runnable() {
              @Override
              public void run() {
                  update();
              }

                // forces refresh
                private void update() {
                    uploadItems.getReadWriteLock().writeLock().lock();
                    try {
                        // TODO use TransactionList for these for performance (requires using GlazedLists from head)
                        for (UploadItem item : uploadItems) {
                            if (item instanceof CoreUploadItem)
                                ((CoreUploadItem) item).fireDataChanged();
                        }
                    } finally {
                        uploadItems.getReadWriteLock().writeLock().unlock();
                    }
                }
           
          };
          backgroundExecutor.scheduleAtFixedRate(command, 0, PERIOD, TimeUnit.MILLISECONDS);
    }

    @Override
    public List<UploadItem> getUploadItems() {
        return uploadItems;
    }

    @Override
    public EventList<UploadItem> getSwingThreadSafeUploads() {
        assert EventQueue.isDispatchThread();
        if (swingThreadUploadItems == null) {
            swingThreadUploadItems = GlazedListsFactory.swingThreadProxyEventList(uploadItems);
        }
        return swingThreadUploadItems;
    }

    @Override
    public void uploadAdded(Uploader uploader) {
        uploadItems.add(new CoreUploadItem(uploader));
    }

    @Override
    public void uploadRemoved(Uploader uploader) {
        uploadItems.remove(new CoreUploadItem(uploader));
    }

}
