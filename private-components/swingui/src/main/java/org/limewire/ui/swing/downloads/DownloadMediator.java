package org.limewire.ui.swing.downloads;

import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DownloadMediator {

	/**
	 * unfiltered - common to all tables
	 */
	private final EventList<DownloadItem> commonBaseList;
	private DownloadListManager downloadListManager;
	
	@Inject
	public DownloadMediator(DownloadListManager downloadManager) {
	    this.downloadListManager = downloadManager;
		commonBaseList = GlazedListsFactory.filterList(downloadManager.getSwingThreadSafeDownloads(), new DownloadStateExcluder(DownloadState.CANCELLED));
	}

	public void pauseAll() {
        commonBaseList.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : commonBaseList) {
                if (item.getState().isPausable()) {
                    item.pause();
                }
            }
        } finally {
            commonBaseList.getReadWriteLock().writeLock().unlock();
        }
    }

	public void resumeAll() {
        commonBaseList.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : commonBaseList) {
                if (item.getState().isResumable()) {
                    item.resume();
                }
            }
        } finally {
            commonBaseList.getReadWriteLock().writeLock().unlock();
        }
    }
	
	public EventList<DownloadItem> getDownloadList() {
	    return this.commonBaseList;
	}
	
	public void clearFinished() {
	    downloadListManager.clearFinished();
	}

    public void fixStalled() {
        commonBaseList.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : commonBaseList) {
                if (item.getState() == DownloadState.STALLED) {
                    item.resume();
                }
            }
        } finally {
            commonBaseList.getReadWriteLock().writeLock().unlock();
        }
    }

    public void cancelStalled() {
        cancelMatchingDownloadItems(DownloadState.STALLED);
    }

    public void cancelError() {
        cancelMatchingDownloadItems(DownloadState.ERROR);
    }

    public void cancelAll() {
        cancelMatchingDownloadItems(null);
    }
    
    /**
     * 
     * @param state The state of the DownloadItems to be canceled.  Null will cancel all.
     */
    private void cancelMatchingDownloadItems(DownloadState state){
        List<DownloadItem> items = getMatchingDownloadItems(state);
        for(DownloadItem item : items){
            item.cancel();
        }
    }
    
    /**
     * 
     * @param state null will return all DownloadItems
     * @return a List of all DownloadItems in the specified DownloadState
     */
    private List<DownloadItem> getMatchingDownloadItems(DownloadState state) {
        if(state == null){
            return new ArrayList<DownloadItem>(commonBaseList);
        }
        List<DownloadItem> matchingItems = new ArrayList<DownloadItem>();
        commonBaseList.getReadWriteLock().readLock().lock();
        try {
            for (DownloadItem item : commonBaseList) {
                if (item.getState() == state) {
                    matchingItems.add(item);
                }
            }
        } finally {
            commonBaseList.getReadWriteLock().readLock().unlock();
        }
        return matchingItems;
    }
}
