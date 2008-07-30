package org.limewire.ui.swing.downloads;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;


class DownloadMediator {

	
	private JTextField searchBar;
	/**
	 * filtered by textbox
	 */
	private EventList<DownloadItem> filteredList;
	
	/**
	 * unfiltered - common to all tables
	 */
	private EventList<DownloadItem> commonBaseList;
	
	public DownloadMediator(DownloadListManager downloadManager) {
	
		commonBaseList= new FilterList<DownloadItem>(downloadManager.getDownloads(), new DownloadStateExcluder(DownloadState.CANCELLED));	
		
		searchBar = new JTextField();
		filteredList = new FilterList<DownloadItem>(commonBaseList, 
				new TextComponentMatcherEditor<DownloadItem>(searchBar, new DownloadItemTextFilterator(), true));		
	}

	

	public void pauseAll() {
	    //TODO use TransactionList for these for performance
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

	public JTextField getFilterBar(){
		return searchBar;
	}
	
	public EventList<DownloadItem> getFilteredList(){
		return filteredList;
	}
	
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
