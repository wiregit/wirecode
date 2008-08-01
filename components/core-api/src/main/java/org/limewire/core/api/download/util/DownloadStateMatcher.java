package org.limewire.core.api.download.util;

import java.util.HashSet;
import java.util.Set;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;

import ca.odell.glazedlists.matchers.Matcher;

public class DownloadStateMatcher implements Matcher<DownloadItem> {

	
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
		if (downloadStates.isEmpty())
			return true;

		return downloadStates.contains(item.getState());
	}

	

}
