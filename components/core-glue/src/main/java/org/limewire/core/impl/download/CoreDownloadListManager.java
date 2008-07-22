package org.limewire.core.impl.download;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadListener;
import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;

import com.google.inject.Inject;
import com.google.inject.name.Named;


public class CoreDownloadListManager implements DownloadListManager{
    
	private EventList<DownloadItem> downloadItems;
    
    private static final int PERIOD = 1000;
	
	@Inject
	public CoreDownloadListManager(DownloadListenerList listenerList, @Named("backgroundExecutor") ScheduledExecutorService backgroundExecutor){
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = GlazedLists.threadSafeList(
	            new ObservableElementList<DownloadItem>(new BasicEventList<DownloadItem>(), downloadConnector));
	    listenerList.addDownloadListener(new CoreDownloadListener(downloadItems));
	    
	  //TODO: change backgroundExecutor to listener - currently no listener for download progress
      //hack to force tables to update
	    Runnable command = new Runnable() {
            @Override
            public void run() {
                update();
            }
        };
        backgroundExecutor.scheduleAtFixedRate(command, 0, PERIOD, TimeUnit.MILLISECONDS);

	}

    // forces refresh
    private void update() {
        downloadItems.getReadWriteLock().writeLock().lock();
        try {
            if (downloadItems.size() > 0) {
                downloadItems.set(0, downloadItems.get(0));
            }
        } finally {
            downloadItems.getReadWriteLock().writeLock().unlock();
        }
    }
	
	@Override
	public EventList<DownloadItem> getDownloads() {
		return downloadItems;
	}

	@Override
	public DownloadItem addDownload(SearchResult... searchResults) {
	    // TODO Auto-generated method stub
	    return null;
	}

	
	private static class CoreDownloadListener implements DownloadListener {
	    
	    private List<DownloadItem> list;

        public CoreDownloadListener(List<DownloadItem> list){
	        this.list = list;
	    }

        @Override
        public void downloadAdded(DownloadItem downloadItem) {
            list.add(downloadItem);
        }

        @Override
        public void downloadRemoved(DownloadItem downloadItem) {
            list.remove(downloadItem);
        }

    }
}
