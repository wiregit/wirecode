package org.limewire.core.impl.download;

import java.util.ArrayList;
import java.util.List;

import org.limewire.core.api.download.DownloadAddedListener;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadManager;
import org.limewire.core.api.download.DownloadState;


public class MockDownloadManager implements DownloadManager {
	private List<DownloadAddedListener> listeners = new ArrayList<DownloadAddedListener>();
	private ArrayList<DownloadItem> downloadItems = new ArrayList<DownloadItem>();
	
	public MockDownloadManager(){
		initializeMockData();
	}

	@Override
	public List<DownloadItem> getDownloads() {
		//defensive copy
		return new ArrayList<DownloadItem>(downloadItems);
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
		downloadItems.add(downloadItem);
		for(DownloadAddedListener listener : listeners){
			listener.downloadAdded(downloadItem);
		}
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

}
