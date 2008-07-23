package org.limewire.core.impl.download;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;


public class MockDownloadListManager implements DownloadListManager {
	private RemoveCancelledListener cancelListener = new RemoveCancelledListener();
	private EventList<DownloadItem> downloadItems;
	
	public MockDownloadListManager(){
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = GlazedLists.threadSafeList(
	            new ObservableElementList<DownloadItem>(new BasicEventList<DownloadItem>(), downloadConnector));
		initializeMockData();
	}

	@Override
	public EventList<DownloadItem> getDownloads() {
		return downloadItems;
	}

	@Override
	public DownloadItem addDownload(Search search, List<? extends SearchResult> coreSearchResults) {
	    // TODO Auto-generated method stub
	    return null;
	}
	
	
	public synchronized void addDownload(DownloadItem downloadItem){
	    downloadItem.addPropertyChangeListener(cancelListener);
		downloadItems.add(downloadItem);
		
	}
	

	private void initializeMockData(){
	    MockDownloadItem item = new MockDownloadItem("Monkey on Skateboard", 44.6,
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
				DownloadState.LOCAL_QUEUED, DownloadItem.Category.DOCUMENT);
		item.addDownloadSource(new MockDownloadSource("Barack"));
		item.addDownloadSource(new MockDownloadSource("Bob"));
		addDownload(item);
		
		item = new MockDownloadItem("other queued file", 5.5,
                DownloadState.REMOTE_QUEUED, DownloadItem.Category.DOCUMENT);
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
