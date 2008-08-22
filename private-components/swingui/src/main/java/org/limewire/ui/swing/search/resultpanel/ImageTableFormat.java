package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * image descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ImageTableFormat extends ResultsTableFormat {

    public ImageTableFormat() {
        columnNames = new String[] {
            "Name", "Type", "Date Created", "Actions", "Relevance",
            "People with File", "Owner"
        };

        vsrIndex = 3;
    }

    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case 0: return get(PropertyKey.NAME);
            case 1: return vsr.getFileExtension();
            case 2: return get(PropertyKey.DATE_CREATED);
            case 3: return vsr;
            case 4: return get(PropertyKey.RELEVANCE);
            case 5: return ""; // people with file
            case 6: return ""; // owner
            default: return null;
        }
    }
}