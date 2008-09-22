package org.limewire.core.impl.download;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadItem.ErrorState;
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
	    ObservableElementList.Connector<DownloadItem> downloadConnector =
            GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = GlazedLists.threadSafeList(
	        new ObservableElementList<DownloadItem>(
            new BasicEventList<DownloadItem>(), downloadConnector));
		initializeMockData();
	}

	@Override
	public EventList<DownloadItem> getDownloads() {
		return downloadItems;
	}

	@Override
	public DownloadItem addDownload(
            Search search, List<? extends SearchResult> coreSearchResults) {
        String title = "title"; // TODO: RMV What should go here?
        long totalSize = 123;
        DownloadState state = DownloadState.DOWNLOADING;
        Category category = Category.AUDIO;
        final MockDownloadItem mdi =
            new MockDownloadItem(title, totalSize, state, category);

        // Simulate a search.
        Runnable runnable = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    // do nothing
                }
                mdi.setState(DownloadState.DONE);
            }
        };

        new Thread(runnable).start();

	    return mdi;
	}
	
	public synchronized void addDownload(DownloadItem downloadItem){
	    downloadItem.addPropertyChangeListener(cancelListener);
		downloadItems.add(downloadItem);
	}
	
	private void initializeMockData(){
	    MockDownloadItem item = new MockDownloadItem("Monkey on Skateboard", 446,
				DownloadState.DOWNLOADING, Category.VIDEO);
		item.addDownloadSource(new MockDownloadSource("Frank"));
		item.addDownloadSource(new MockDownloadSource("Bob"));
		item.addDownloadSource(new MockDownloadSource("Joey"));
		addDownload(item);
		
		item = new MockDownloadItem("FINISHING", 446,
				DownloadState.FINISHING, Category.AUDIO);
		item.addDownloadSource(new MockDownloadSource("Henry"));
		addDownload(item);
		
		item = new MockDownloadItem("done on Skateboard image", 446,
				DownloadState.DONE, Category.IMAGE);
		item.addDownloadSource(new MockDownloadSource("Jolene"));
		item.addDownloadSource(new MockDownloadSource("Bob"));
		addDownload(item);
		
		item = new MockDownloadItem("queued video", 55,
				DownloadState.LOCAL_QUEUED, Category.VIDEO);
		item.addDownloadSource(new MockDownloadSource("Barack"));
		item.addDownloadSource(new MockDownloadSource("Bob"));
		addDownload(item);
		
		item = new MockDownloadItem("other queued doc", 55,
                DownloadState.REMOTE_QUEUED, Category.DOCUMENT);
        item.addDownloadSource(new MockDownloadSource("Barack"));
        item.addDownloadSource(new MockDownloadSource("Bob"));
        addDownload(item);
		
		item = new MockDownloadItem("Paused audio file", 55,
				DownloadState.PAUSED, Category.AUDIO);
		item.addDownloadSource(new MockDownloadSource("John"));
		item.addDownloadSource(new MockDownloadSource("George"));
		addDownload(item);
        
        item = new MockDownloadItem("Stalled program", 55,
                DownloadState.STALLED, Category.PROGRAM);
        item.addDownloadSource(new MockDownloadSource("Al"));
        item.addDownloadSource(new MockDownloadSource("Bob"));
        addDownload(item);
        
        item = new MockDownloadItem("Corrupt other file", 55,
                DownloadState.ERROR, Category.OTHER);
        item.addDownloadSource(new MockDownloadSource("Al"));
        item.addDownloadSource(new MockDownloadSource("Bob"));
        item.setErrorState(ErrorState.CORRUPT_FILE);
        addDownload(item);
        
        item = new MockDownloadItem("disk problem video", 55,
                DownloadState.ERROR, Category.VIDEO);
        item.addDownloadSource(new MockDownloadSource("Al"));
        item.addDownloadSource(new MockDownloadSource("Bob"));
        item.setErrorState(ErrorState.DISK_PROBLEM);
        addDownload(item);
        
        item = new MockDownloadItem("not sharable video", 55,
                DownloadState.ERROR, Category.VIDEO);
        item.addDownloadSource(new MockDownloadSource("Al"));
        item.addDownloadSource(new MockDownloadSource("Bob"));
        item.setErrorState(ErrorState.FILE_NOT_SHARABLE);
        addDownload(item);
        
        item = new MockDownloadItem("UNABLE_TO_CONNECT vid", 55,
                DownloadState.ERROR, Category.VIDEO);
        item.addDownloadSource(new MockDownloadSource("Al"));
        item.addDownloadSource(new MockDownloadSource("Bob"));
        item.setErrorState(ErrorState.UNABLE_TO_CONNECT);
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