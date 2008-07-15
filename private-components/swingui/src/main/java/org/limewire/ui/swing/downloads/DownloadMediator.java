package org.limewire.ui.swing.downloads;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

import org.limewire.core.api.download.DownloadAddedListener;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadManager;
import org.limewire.core.api.download.DownloadState;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Injector;

public class DownloadMediator {

	
	private RemoveCancelledListener cancelledListener = new RemoveCancelledListener();

	private JTextField searchBar;
	/**
	 * filtered by textbox
	 */
	private EventList<DownloadItem> filteredList;
	//TODO:synchronization for list
	/**
	 * unfiltered - common to all tables
	 */
	private EventList<DownloadItem> commonBaseList;
	
	public DownloadMediator(DownloadManager downloadManager) {
		ObservableElementList.Connector<DownloadItem> downloadConnector = GlazedLists.beanConnector(DownloadItem.class);
		//shared by all models
		 commonBaseList = new ObservableElementList<DownloadItem>(new BasicEventList<DownloadItem>(), downloadConnector);	

		List<DownloadItem> items = downloadManager.getDownloads();
		downloadManager.addDownloadAddedListener(new DownloadAddedListener() {
			@Override
			public void downloadAdded(DownloadItem downloadItem) {
				addDownloadItem(downloadItem);
			}
		});
		
		for (DownloadItem item : items) {
			addDownloadItem(item);
		}
		
		
		searchBar = new JTextField();
		filteredList = new FilterList<DownloadItem>(commonBaseList, 
				new TextComponentMatcherEditor<DownloadItem>(searchBar, new DownloadItemTextFilterator(), true));
		
	}

	private void addDownloadItem(DownloadItem downloadItem) {
		downloadItem.addPropertyChangeListener(cancelledListener);
		commonBaseList.getReadWriteLock().writeLock().lock();
		try {
			commonBaseList.add(downloadItem);
		} finally {
			commonBaseList.getReadWriteLock().writeLock().unlock();
		}
	}

	

	public void pauseAll() {
		for (DownloadItem item : commonBaseList) {
			if (item.getState().isPausable()) {
				item.pause();
			}
		}
	}


	public void resumeAll(){
		for (DownloadItem item : commonBaseList) {
			if (item.getState().isResumable()) {
				item.resume();
			}
		}
	}
	
	public void clearFinished() {
		List<DownloadItem> finishedItems = new ArrayList<DownloadItem>();

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
	
	private class RemoveCancelledListener implements PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if(evt.getNewValue() == DownloadState.CANCELLED){
				commonBaseList.getReadWriteLock().writeLock().lock();
				try {
					commonBaseList.remove(evt.getSource());
				} finally {
					commonBaseList.getReadWriteLock().writeLock().unlock();
				}
			}
		}

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
