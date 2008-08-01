package org.limewire.ui.swing.downloads.table;


import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;


public class DownloadStateExcluder extends DownloadStateMatcher {

	public DownloadStateExcluder(DownloadState... excludedStates) {
		super(excludedStates);
	}
	
	@Override
	public boolean matches(DownloadItem item) {
		if (item == null)
			return false;

		return !super.matches(item);
	}

	

}
