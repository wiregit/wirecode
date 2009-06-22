package org.limewire.ui.swing.downloads;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.util.FileUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DownloadMediator {
    
    public static enum SortOrder {ORDER_ADDED_DOWN, ORDER_ADDED_UP, NAME_DOWN, NAME_UP, PROGRESS_DOWN, PROGRESS_UP, 
        TIME_REMAINING_DOWN, TIME_REMAINING_UP, SPEED_DOWN, SPEED_UP, STATUS_DOWN, STATUS_UP, FILE_TYPE_DOWN, FILE_TYPE_UP, EXTENSION_DOWN, EXTENSION_UP};

	/**
	 * unfiltered - common to all tables
	 */
    private final SortedList<DownloadItem> commonBaseList;
	private DownloadListManager downloadListManager;
	
	@Inject
	public DownloadMediator(DownloadListManager downloadManager) {
	    this.downloadListManager = downloadManager;
	    EventList<DownloadItem> baseList = GlazedListsFactory.filterList(downloadManager.getSwingThreadSafeDownloads(), new DownloadStateExcluder(DownloadState.CANCELLED));
	    commonBaseList = GlazedListsFactory.sortedList(baseList, new DescendingOrderAddedComparator());
	    
	}
	
	public void setSortOrder(SortOrder order){
	    switch(order){
        case ORDER_ADDED_DOWN:
            commonBaseList.setComparator(new DescendingOrderAddedComparator());
            break;
        case ORDER_ADDED_UP:
            commonBaseList.setComparator(new AscendingOrderAddedComparator());
            break;
        case NAME_DOWN:
            commonBaseList.setComparator(new DescendingNameComparator());
            break;
        case NAME_UP:
            commonBaseList.setComparator(new AscendingNameComparator());
            break;
        case PROGRESS_DOWN:
            commonBaseList.setComparator(new DescendingProgressComparator());
            break;
        case PROGRESS_UP:
            commonBaseList.setComparator(new AscendingProgressComparator());
            break;
        case TIME_REMAINING_DOWN:
            commonBaseList.setComparator(new DescendingTimeRemainingComparator());
            break;
        case TIME_REMAINING_UP:
            commonBaseList.setComparator(new AscendingTimeRemainingComparator());
            break;
        case SPEED_DOWN:
            commonBaseList.setComparator(new DescendingSpeedComparator());
            break;
        case SPEED_UP:
            commonBaseList.setComparator(new AscendingSpeedComparator());
            break;
        case STATUS_DOWN:
            commonBaseList.setComparator(new StateComparator());
            break;
        case STATUS_UP:
            commonBaseList.setComparator(new AscendingStateComparator());
            break;
        case FILE_TYPE_DOWN:
            commonBaseList.setComparator(new DescendingFileTypeComparator());
            break;
        case FILE_TYPE_UP:
            commonBaseList.setComparator(new AscendingFileTypeComparator());
            break;
        case EXTENSION_DOWN:
            commonBaseList.setComparator(new DescendingFileExtensionComparator());
            break;
        case EXTENSION_UP:
            commonBaseList.setComparator(new AscendingFileExtensionComparator());
            break;
	    }
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
        List<DownloadItem> items = getMatchingDownloadItems(DownloadState.STALLED);
        for (DownloadItem item : items) {
            item.resume();
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
        if (state == null) {
            return new ArrayList<DownloadItem>(commonBaseList);
        }
        
        List<DownloadItem> matchingItems = new ArrayList<DownloadItem>();
        for (DownloadItem item : commonBaseList) {
            if (item.getState() == state) {
                matchingItems.add(item);
            }
        }
        
        return matchingItems;
    }

    private static class StateComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return getSortPriority(o1.getState()) - getSortPriority(o2.getState());
        }  
    }
    
    private static class AscendingStateComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return getSortPriority(o1.getState()) - getSortPriority(o2.getState());
        }  
    }
    
    private static int getSortPriority(DownloadState state){
        
        switch(state){
        case DONE: return 1;
        case FINISHING: return 2;
        case DOWNLOADING: return 3;
        case RESUMING: return 4;
        case CONNECTING: return 5;
        case PAUSED: return 6;
        case REMOTE_QUEUED: return 7;
        case LOCAL_QUEUED: return 8;
        case TRYING_AGAIN: return 9;
        case STALLED: return 10;
        case ERROR: return 11;       
        case CANCELLED: return 12;
        }
        
       throw new IllegalArgumentException("Unknown DownloadState: " + state);
    }
    
    private static class DescendingOrderAddedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) { 
            if (o1 == o2){
                return 0;
            }
            return o1.getStartDate().compareTo(o2.getStartDate());
        }      
    }
    
    private static class AscendingOrderAddedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) { 
            if (o1 == o2){
                return 0;
            }
            return o2.getStartDate().compareTo(o1.getStartDate());
        }      
    }
    
    private static class DescendingNameComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getTitle().compareTo(o2.getTitle());
        }   
     
    } 
    
    private static class AscendingNameComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o2.getTitle().compareTo(o1.getTitle());
        }   
     
    } 
    
    private static class DescendingProgressComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o2.getPercentComplete() - o1.getPercentComplete();
        }   
     
    } 
    
    private static class AscendingProgressComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getPercentComplete() - o2.getPercentComplete();
        }   
     
    } 
    
    private static class DescendingTimeRemainingComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)(o1.getRemainingDownloadTime() - o2.getRemainingDownloadTime());
        }   
     
    }
    
    private static class AscendingTimeRemainingComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)(o2.getRemainingDownloadTime() - o1.getRemainingDownloadTime());
        }   
     
    }
    
    
    private static class DescendingSpeedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)o2.getDownloadSpeed() - (int)o1.getDownloadSpeed();
        }   
     
    }
    
    
    private static class AscendingSpeedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)o1.getDownloadSpeed() - (int)o2.getDownloadSpeed();
        }   
     
    }
    
    
    private static class DescendingFileTypeComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getCategory().compareTo(o2.getCategory());
        }   
     
    }
    
    
    private static class AscendingFileTypeComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o2.getCategory().compareTo(o1.getCategory());
        }   
     
    }
    
    
    private static class DescendingFileExtensionComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return FileUtils.getFileExtension(o1.getDownloadingFile()).compareTo(FileUtils.getFileExtension(o2.getDownloadingFile()));
        }   
     
    }
    
    private static class AscendingFileExtensionComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return FileUtils.getFileExtension(o2.getDownloadingFile()).compareTo(FileUtils.getFileExtension(o1.getDownloadingFile()));
        }   
     
    }
}
