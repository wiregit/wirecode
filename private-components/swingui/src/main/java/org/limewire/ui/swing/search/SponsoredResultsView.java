package org.limewire.ui.swing.search;

import java.net.URL;
import java.util.List;

import org.limewire.core.api.search.sponsored.SponsoredResult;

/**
 * Defines a view of the search results.
 */
public interface SponsoredResultsView {

    /**
     * Adds the specified list of sponsored results to the display.
     */
    public void addSponsoredResults(List<? extends SponsoredResult> sponsoredResults);
    
    /**
     * Adds the spoon ad to the results in the display.
     */
    public void addSpoonResult(URL url);
}
