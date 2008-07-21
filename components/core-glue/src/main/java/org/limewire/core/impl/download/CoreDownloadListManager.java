package org.limewire.core.impl.download;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


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

    private Timer timer;
    
    private static final int DELAY = 1000;
	
	@Inject
	public CoreDownloadListManager(DownloadListenerList listenerList){
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = GlazedLists.threadSafeList(
	            new ObservableElementList<DownloadItem>(new BasicEventList<DownloadItem>(), downloadConnector));
	    listenerList.addDownloadListener(new CoreDownloadListener(downloadItems));
	    
	    //TODO: change timer to listener - currently no listener for download progress
	    //hack to force tables to update
        timer = new Timer("CoreDownloadListManager-Timer", true);
        
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                synchronized (downloadItems) {
                    if (downloadItems.size() > 0) {
                        downloadItems.set(0, downloadItems.get(0));
                    }
                }
            }
        };
            
        timer.scheduleAtFixedRate(task, 0, DELAY);
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
