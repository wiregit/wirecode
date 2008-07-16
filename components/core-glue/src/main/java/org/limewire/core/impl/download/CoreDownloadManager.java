package org.limewire.core.impl.download;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.download.DownloadAddedListener;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadManager;
import org.limewire.core.api.download.DownloadState;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;


public class CoreDownloadManager implements DownloadManager {
	private List<DownloadAddedListener> listeners = new ArrayList<DownloadAddedListener>();
	private RemoveCancelledListener cancelListener = new RemoveCancelledListener();
	private ObservableElementList<DownloadItem> downloadItems;
	
	public CoreDownloadManager(){
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = new ObservableElementList<DownloadItem>(new BasicEventList<DownloadItem>(), downloadConnector);
	}

	@Override
	public EventList<DownloadItem> getDownloads() {
		return downloadItems;
	}

	@Override
	public synchronized void addDownloadAddedListener(DownloadAddedListener listener) {
		listeners.add(listener);
	}

	@Override
	public synchronized void removeDownloadAddedListener(DownloadAddedListener listener) {
		listeners.remove(listener);
	}
	
	public synchronized void addDownload(DownloadItem downloadItem){
	    downloadItem.addPropertyChangeListener(cancelListener);
		downloadItems.add(downloadItem);
		for(DownloadAddedListener listener : listeners){
			listener.downloadAdded(downloadItem);
		}
	}
	
	
	
	private class RemoveCancelledListener implements PropertyChangeListener {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue() == DownloadState.CANCELLED) {
                // TODO : proper concurrency
                downloadItems.getReadWriteLock().writeLock().lock();
                try {
                    downloadItems.remove(evt.getSource());
                } finally {
                    downloadItems.getReadWriteLock().writeLock().unlock();
                }
            }
        }

    }

}
