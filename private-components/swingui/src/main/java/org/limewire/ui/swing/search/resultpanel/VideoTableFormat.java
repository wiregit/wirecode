package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * video descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class VideoTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public VideoTableFormat() {
        columnNames = new String[] {
        "Title", "Type", "Length", "Year", "Quality",
        "Actions", "Relevance", "People with File", "Owner", "Rating",
        "Comments", "Height", "Width", "Bitrate"
        };

        actionColumnIndex = 5;
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case 0: return get(PropertyKey.NAME);
            case 1: return vsr.getFileExtension();
            case 2: return get(PropertyKey.LENGTH);
            case 3: return get(PropertyKey.YEAR);
            case 4: return get(PropertyKey.QUALITY);
            case 5: return vsr;
            case 6: return get(PropertyKey.RELEVANCE);
            case 7: return ""; // people with file
            case 8: return ""; // owner
            case 9: return get(PropertyKey.RATING);
            case 10: return get(PropertyKey.COMMENTS);
            case 11: return get(PropertyKey.HEIGHT);
            case 12: return get(PropertyKey.WIDTH);
            case 13: return get(PropertyKey.BITRATE);
            default: return null;
        }
    }
}