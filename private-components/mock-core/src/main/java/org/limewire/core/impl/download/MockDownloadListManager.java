package org.limewire.core.impl.download;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.DownloadItem.DownloadItemType;
import org.limewire.core.api.download.DownloadItem.ErrorState;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;
import org.limewire.listener.SwingSafePropertyChangeSupport;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;

import com.google.inject.Singleton;

@Singleton
public class MockDownloadListManager implements DownloadListManager {
    private final EventList<DownloadItem> threadSafeDownloadItems;
    private final EventList<DownloadItem> observableDownloadItems;
    private final RemoveCancelledListener cancelListener = new RemoveCancelledListener();
    private final PropertyChangeSupport changeSupport = new SwingSafePropertyChangeSupport(this);
    
    private EventList<DownloadItem> swingThreadDownloadItems;
	
	public MockDownloadListManager(){
        threadSafeDownloadItems = GlazedLists.threadSafeList(new BasicEventList<DownloadItem>());
	    ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
	    observableDownloadItems = GlazedListsFactory.observableElementList(threadSafeDownloadItems, downloadConnector);
		initializeMockData();
	}

	@Override
	public EventList<DownloadItem> getDownloads() {
		return observableDownloadItems;
	}
	
	@Override
	public EventList<DownloadItem> getSwingThreadSafeDownloads() {
        if (swingThreadDownloadItems == null) {
            swingThreadDownloadItems = GlazedListsFactory.swingThreadProxyEventList(observableDownloadItems);
        }
        return swingThreadDownloadItems;
	}

	@Override
	public DownloadItem addDownload(
            Search search, List<? extends SearchResult> coreSearchResults) {
        String title = coreSearchResults.get(0).getFileName(); 
        long totalSize = coreSearchResults.get(0).getSize();
        DownloadState state = DownloadState.DOWNLOADING;
        Category category = coreSearchResults.get(0).getCategory();
        final MockDownloadItem mdi =
            new MockDownloadItem(title, totalSize, state, category);

        addDownload(mdi);
	    return mdi;
	}

	public void addDownload(DownloadItem downloadItem){
	    downloadItem.addPropertyChangeListener(cancelListener);
		threadSafeDownloadItems.add(downloadItem);
		changeSupport.firePropertyChange(DOWNLOAD_ADDED, false, true);
	}
	
	private void initializeMockData(){
	    MockDownloadItem item = new MockDownloadItem(DownloadItemType.ANTIVIRUS,
	            "Anti-virus definitions", 4416, DownloadState.DOWNLOADING, 
	            Category.OTHER);
		item.addDownloadSource(new MockDownloadSource("134.23.2.7"));
		addDownload(item);

		item = new MockDownloadItem("Psychology 101 Lecture 3", 446,
				DownloadState.DOWNLOADING, Category.AUDIO);
		item.addDownloadSource(new MockDownloadSource("245.2.7.78"));
		addDownload(item);
		
		item = new MockDownloadItem("New England Foliage.bmp", 46,
				DownloadState.DOWNLOADING, Category.IMAGE);
		item.addDownloadSource(new MockDownloadSource("234.2.3.4"));
		addDownload(item);

        item = new MockDownloadItem("Funky file.exe", 446,
                DownloadState.THREAT_FOUND, Category.PROGRAM);
        item.addDownloadSource(new MockDownloadSource("245.2.7.78"));
        addDownload(item);

		item = new MockDownloadItem("Psychology 101 Lecture 2.avi", 55,
				DownloadState.SCAN_FAILED, Category.VIDEO);
		item.addDownloadSource(new MockDownloadSource("34.2.7.7"));
		addDownload(item);

        item = new MockDownloadItem("Psychology 101 Lecture 1", 55,
                DownloadState.ERROR, Category.VIDEO);
        item.addDownloadSource(new MockDownloadSource("23.12.33.4"));
        item.setErrorState(ErrorState.DISK_PROBLEM);
        addDownload(item);
        
        item = new MockDownloadItem("Psychology 102 Lecture 1", 55,
                DownloadState.CONNECTING, Category.VIDEO);
        item.addDownloadSource(new MockDownloadSource("23.12.33.4"));
        addDownload(item);
        
        item = new MockDownloadItem("Cancun.bmp", 232753,
                DownloadState.STALLED, Category.AUDIO);
        item.setCurrentSize(113472);
        item.addDownloadSource(new MockDownloadSource("234.2.2.2"));
        addDownload(item);

	}
	
	private class RemoveCancelledListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Object newValue = evt.getNewValue();
            if (newValue == DownloadState.CANCELLED) {
                threadSafeDownloadItems.remove(evt.getSource());
            } else if (newValue == DownloadState.DONE ||
                    newValue == DownloadState.DANGEROUS ||
                    newValue == DownloadState.THREAT_FOUND ||
                    newValue == DownloadState.SCAN_FAILED) {
                changeSupport.firePropertyChange(DOWNLOAD_COMPLETED, null, evt.getSource());
            }
        }
    }

    @Override
    public DownloadItem addTorrentDownload(URI uri, boolean overwrite) throws DownloadException {
        return null;
    }

    @Override
    public DownloadItem addDownload(Search search, List<? extends SearchResult> coreSearchResults,
            File saveFile, boolean overwrite) throws DownloadException {
       return addDownload(search, coreSearchResults);
    }

    @Override
    public DownloadItem addTorrentDownload(File file, File saveDirectory, boolean overwrite)
            throws DownloadException {
        return null;
    }

    @Override
    public boolean contains(URN urn) {
        return false;
    }

    @Override
    public DownloadItem addDownload(MagnetLink magnet, File saveFile, boolean overwrite)
            throws DownloadException {
        return null;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    
    @Override
    public void updateDownloadsCompleted() {
    }

    @Override
    public DownloadItem getDownloadItem(URN urn) {
        return null;
    }
    
    @Override
    public void clearFinished() {
        List<DownloadItem> finishedItems = new ArrayList<DownloadItem>();
        threadSafeDownloadItems.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : threadSafeDownloadItems) {
                DownloadState state = item.getState();
                if (state == DownloadState.DONE ||
                        state == DownloadState.DANGEROUS ||
                        state == DownloadState.THREAT_FOUND ||
                        state == DownloadState.SCAN_FAILED) {
                    finishedItems.add(item);
                }
            }
            
            for (DownloadItem item : finishedItems) {
                threadSafeDownloadItems.remove(item);
            }
        } finally {
            threadSafeDownloadItems.getReadWriteLock().writeLock().unlock();
        }
    }
    
    @Override
    public void remove(DownloadItem item) {
        threadSafeDownloadItems.remove(item);
    }
    
}
