package org.limewire.core.api.download;


import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.EventList;

public interface DownloadListManager {
	
	public EventList<DownloadItem> getDownloads();
	
	   
    /**
     * Adds a download triggered by the given search results.
     * The search results must all be for the same item,
     * otherwise an {@link IllegalArgumentException} may be thrown.
     */
    public DownloadItem addDownload(SearchResult... searchResults);
    
    
	
	
}
