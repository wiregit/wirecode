package org.limewire.core.api.download.util;

import java.util.HashSet;
import java.util.Set;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;

import ca.odell.glazedlists.matchers.Matcher;

public class DownloadStateExcluder implements Matcher<DownloadItem> {

	
	private Set<DownloadState> excludedDownloadStates = new HashSet<DownloadState>();

	
	public DownloadStateExcluder(DownloadState... exludedStates) {
		for (DownloadState state : exludedStates) {
			excludedDownloadStates.add(state);
		}
	}

	
	@Override
	public boolean matches(DownloadItem item) {
		if (item == null)
			return false;
		if (excludedDownloadStates.isEmpty())
			return true;

		return !excludedDownloadStates.contains(item.getState());
	}

	

}
