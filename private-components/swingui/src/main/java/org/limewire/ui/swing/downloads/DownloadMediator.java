package org.limewire.ui.swing.downloads;

import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;

import ca.odell.glazedlists.EventList;


class DownloadMediator {

	/**
	 * unfiltered - common to all tables
	 */
	private final EventList<DownloadItem> commonBaseList;
	
	public DownloadMediator(DownloadListManager downloadManager) {
		commonBaseList= GlazedListsFactory.filterList(downloadManager.getSwingThreadSafeDownloads(), new DownloadStateExcluder(DownloadState.CANCELLED));
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
	
	public EventList<DownloadItem> getDownloadList() {
	    return this.commonBaseList;
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
}
