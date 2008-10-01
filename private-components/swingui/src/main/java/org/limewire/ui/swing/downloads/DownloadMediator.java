package org.limewire.ui.swing.downloads;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.search.FilteredTextField;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;


class DownloadMediator {

	
	private JTextField filterField;
	
	/**
	 * filtered by filterField
	 */
	private EventList<DownloadItem> filteredList;
	
	/**
	 * unfiltered - common to all tables
	 */
	private EventList<DownloadItem> commonBaseList;
	
	public DownloadMediator(DownloadListManager downloadManager) {
	
		commonBaseList= GlazedListsFactory.filterList(downloadManager.getDownloads(), new DownloadStateExcluder(DownloadState.CANCELLED));	
		commonBaseList = GlazedListsFactory.threadSafeList(commonBaseList);
		filterField = new FilteredTextField(10);
		filteredList = GlazedListsFactory.filterList(commonBaseList, 
				new TextComponentMatcherEditor<DownloadItem>(filterField, new DownloadItemTextFilterator(), true));		
	}

	

	public void pauseAll() {
	 // TODO use TransactionList for performance (requires using GlazedLists from head)
        // lock list to ensure it is not modified elsewhere
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
        // lock list to ensure it is not modified elsewhere
	 // TODO use TransactionList for these for performance (requires using GlazedLists from head)
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
	
	public void clearFinished() {
		List<DownloadItem> finishedItems = new ArrayList<DownloadItem>();
		//lock list to ensure it is not modified elsewhere
		commonBaseList.getReadWriteLock().writeLock().lock();
		
		try {
			
			for (DownloadItem item : commonBaseList) {
				if (item.getState() == DownloadState.DONE) {
					finishedItems.add(item);
				}
			}
			
			for(DownloadItem removeItem : finishedItems){
				commonBaseList.remove(removeItem);
			}
			
		} finally {
			commonBaseList.getReadWriteLock().writeLock().unlock();
		}
	}

    /**
     * @return the text field filtering the filtered list
     */
	public JTextField getFilterTextField(){
		return filterField;
	}
	
	/**
	 * @return a Swing thread safe list of DownloadItems filtered by the text field
	 * @see getFilterBar()
	 */
	public EventList<DownloadItem> getFilteredList(){
		return filteredList;
	}
	
	/**
	 * 
	 * @return a Swing thread safe list of all DownloadItems
	 */
	public EventList<DownloadItem> getUnfilteredList(){
		return commonBaseList;
	}
	

	
    private static class DownloadItemTextFilterator implements TextFilterator<DownloadItem> {
        @Override
        public void getFilterStrings(List<String> baseList, DownloadItem element) {
            baseList.add(element.getTitle());
            baseList.add(element.getCategory().toString());
            //TODO: DownloadSources
          //  for(DownloadSource source : element.getDowloadSources())
        }
    }
}
