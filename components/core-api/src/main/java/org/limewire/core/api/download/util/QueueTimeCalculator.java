package org.limewire.core.api.download.util;

import java.util.Comparator;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;

/**
 * calculates
 */
public class QueueTimeCalculator {
    
    /**
     * List of all items whose DownloadState is DOWNLOADING
     */
    private EventList<DownloadItem> downloadingList;

    public QueueTimeCalculator(EventList<DownloadItem> downloadItems) {
       

        downloadingList = new FilterList<DownloadItem>(downloadItems, new DownloadStateMatcher(
                DownloadState.DOWNLOADING));

        Comparator<DownloadItem> dlComparator = new Comparator<DownloadItem>() {
            @Override
            public int compare(DownloadItem o1, DownloadItem o2) {
                return (int) (o1.getRemainingDownloadTime() - o2.getRemainingDownloadTime());
            }
        };

        downloadingList = new SortedList<DownloadItem>(downloadingList, dlComparator);

    }

    public long getRemainingQueueTime(DownloadItem queueItem) {
        
        if(queueItem.getState() != DownloadState.LOCAL_QUEUED){
            return DownloadItem.UNKNOWN_TIME;
        }
        
        int priority = queueItem.getLocalQueuePriority();
        //top priority is 1
        int index = priority - 1;
        
        downloadingList.getReadWriteLock().readLock().lock();
        try {
            if (index >= downloadingList.size()) {
                return DownloadItem.UNKNOWN_TIME;
            }
            return downloadingList.get(index).getRemainingDownloadTime();
        } finally {
            downloadingList.getReadWriteLock().readLock().unlock();
        }
    }

}
