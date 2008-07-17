package org.limewire.core.impl.download;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.download.DownloadListener;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadManager;
import org.limewire.core.api.download.DownloadState;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;


public class MockDownloadManager implements DownloadManager {
	private RemoveCancelledListener cancelListener = new RemoveCancelledListener();
	private EventList<DownloadItem> downloadItems;
	
	public MockDownloadManager(){
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = GlazedLists.threadSafeList(
	            new ObservableElementList<DownloadItem>(new BasicEventList<DownloadItem>(), downloadConnector));
		initializeMockData();
	}

	@Override
	public EventList<DownloadItem> getDownloads() {
		return downloadItems;
	}

	
	
	public synchronized void addDownload(DownloadItem downloadItem){
	    downloadItem.addPropertyChangeListener(cancelListener);
		downloadItems.add(downloadItem);
		
	}
	

	private void initializeMockData(){
		DownloadItem item = new MockDownloadItem("Monkey on Skateboard", 44.6,
				DownloadState.DOWNLOADING, DownloadItem.Category.VIDEO);
		item.addDownloadSource(new MockDownloadSource("Frank"));
		item.addDownloadSource(new MockDownloadSource("Bob"));
		item.addDownloadSource(new MockDownloadSource("Joey"));
		addDownload(item);
		
		item = new MockDownloadItem("FINISHING", 44.6,
				DownloadState.FINISHING, DownloadItem.Category.AUDIO);
		item.addDownloadSource(new MockDownloadSource("Henry"));
		addDownload(item);
		
		item = new MockDownloadItem("done on Skateboard", 44.6,
				DownloadState.DONE, DownloadItem.Category.IMAGE);
		item.addDownloadSource(new MockDownloadSource("Jolene"));
		item.addDownloadSource(new MockDownloadSource("Bob"));
		addDownload(item);
		
		item = new MockDownloadItem("queued file", 5.5,
				DownloadState.QUEUED, DownloadItem.Category.DOCUMENT);
		item.addDownloadSource(new MockDownloadSource("Barack"));
		item.addDownloadSource(new MockDownloadSource("Bob"));
		addDownload(item);
		
		item = new MockDownloadItem("Paused file", 5.5,
				DownloadState.PAUSED, DownloadItem.Category.AUDIO);
		item.addDownloadSource(new MockDownloadSource("John"));
		item.addDownloadSource(new MockDownloadSource("George"));
		addDownload(item);
		
		item = new MockDownloadItem("Stalled file", 5.5,
				DownloadState.STALLED, DownloadItem.Category.VIDEO);
		item.addDownloadSource(new MockDownloadSource("Al"));
		item.addDownloadSource(new MockDownloadSource("Bob"));
		addDownload(item);
	}
	
	private class RemoveCancelledListener implements PropertyChangeListener {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue() == DownloadState.CANCELLED) {
                downloadItems.remove(evt.getSource());
            }
        }

    }

}
