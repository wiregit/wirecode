package org.limewire.ui.swing.search;

import ca.odell.glazedlists.EventList;
import org.limewire.core.api.search.Search;
import org.limewire.ui.swing.search.model.VisualSearchResult;

public interface ResultsContainerFactory {

    public ResultsContainer create(
        EventList<VisualSearchResult> visualSearchResults,
        Search search);
}