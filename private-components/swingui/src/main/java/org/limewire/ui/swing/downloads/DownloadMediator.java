package org.limewire.ui.swing.downloads;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTextField;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;


class DownloadMediator {

	
	private final JTextField filterField;
	
	/**
	 * filtered by filterField
	 */
	private final EventList<DownloadItem> filteredList;
	
	/**
	 * unfiltered - common to all tables
	 */
	private final EventList<DownloadItem> commonBaseList;
	
	public DownloadMediator(DownloadListManager downloadManager) {
		commonBaseList= GlazedListsFactory.filterList(downloadManager.getSwingThreadSafeDownloads(), new DownloadStateExcluder(DownloadState.CANCELLED));
		filterField = new PromptTextField(I18n.tr("Filter"));
        filteredList = GlazedListsFactory.filterList(commonBaseList, 
                new TextComponentMatcherEditor<DownloadItem>(filterField, new DownloadItemTextFilterator(), true));
	}

	public void pauseAll() {
	    for (DownloadItem item : commonBaseList) {
            if (item.getState().isPausable()) {
                item.pause();
            }
        }
    }

	public void resumeAll() {
        for (DownloadItem item : commonBaseList) {
            if (item.getState().isResumable()) {
                item.resume();
            }
        }
    }
	
	public void clearFinished() {
		List<DownloadItem> finishedItems = new ArrayList<DownloadItem>();
	    for (DownloadItem item : commonBaseList) {
			if (item.getState() == DownloadState.DONE) {
				finishedItems.add(item);
			}
		}
		
	    commonBaseList.removeAll(finishedItems);
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
