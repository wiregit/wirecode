package org.limewire.core.impl.download;

import java.util.List;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListener;
import org.limewire.core.api.download.DownloadListManager;

import com.google.inject.Inject;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;


public class CoreDownloadListManager implements DownloadListManager{
    
	private EventList<DownloadItem> downloadItems;
	
	@Inject
	public CoreDownloadListManager(DownloadListenerList listenerList){
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = GlazedLists.threadSafeList(
	            new ObservableElementList<DownloadItem>(new BasicEventList<DownloadItem>(), downloadConnector));
	    listenerList.addDownloadListener(new CoreDownloadListener(downloadItems));
	}

	@Override
	public EventList<DownloadItem> getDownloads() {
		return downloadItems;
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
