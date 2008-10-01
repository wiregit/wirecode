package org.limewire.core.impl.download;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.matchers.Matcher;

/**
 * calculates
 */
public class QueueTimeCalculator {
    
    /**
     * List of all items whose DownloadState is DOWNLOADING
     */
    private EventList<DownloadItem> downloadingList;

    public QueueTimeCalculator(EventList<DownloadItem> downloadItems) {
       

        downloadingList = GlazedListsFactory.filterList(downloadItems, new DownloadStateMatcher(
                DownloadState.DOWNLOADING));

        Comparator<DownloadItem> dlComparator = new Comparator<DownloadItem>() {
            @Override
            public int compare(DownloadItem o1, DownloadItem o2) {
                return (int) (o1.getRemainingDownloadTime() - o2.getRemainingDownloadTime());
            }
        };

        downloadingList = GlazedListsFactory.sortedList(downloadingList, dlComparator);

    }

    public long getRemainingQueueTime(DownloadItem queueItem) {

        downloadingList.getReadWriteLock().readLock().lock();
        
        if(queueItem.getState() != DownloadState.LOCAL_QUEUED){
            return DownloadItem.UNKNOWN_TIME;
        }
        
        int priority = queueItem.getLocalQueuePriority();
        //top priority is 1 (but may briefly be 0 when resuming)
        int index = priority - 1;
        
        try {            
            if (index >= downloadingList.size() || index < 0) {
                return DownloadItem.UNKNOWN_TIME;
            }
            return downloadingList.get(index).getRemainingDownloadTime();
        } finally {
            downloadingList.getReadWriteLock().readLock().unlock();
        }
    }
    
    private static class DownloadStateMatcher implements Matcher<DownloadItem> {

        
        private Set<DownloadState> downloadStates = new HashSet<DownloadState>();

        
        public DownloadStateMatcher(DownloadState... states) {
            for (DownloadState state : states) {
                downloadStates.add(state);
            }
        }

        
        @Override
        public boolean matches(DownloadItem item) {
            if (item == null)
                return false;

            return downloadStates.contains(item.getState());
        }

        

    }

}
