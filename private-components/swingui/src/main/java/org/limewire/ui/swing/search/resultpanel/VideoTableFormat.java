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
        
        String fileExtension = vsr.getFileExtension();

        switch (index) {
            case 0: return getProperty(PropertyKey.NAME);
            case 1: return fileExtension; // TODO: RMV improve
            case 2: return getProperty(PropertyKey.LENGTH);
            case 3: return getProperty(PropertyKey.YEAR);
            case 4: return getProperty(PropertyKey.QUALITY);
            case 5: return vsr;
            case 6: return getProperty(PropertyKey.RELEVANCE);
            case 7: return ""; // people with file
            case 8: return getProperty(PropertyKey.OWNER);
            case 9: return getProperty(PropertyKey.RATING);
            case 10: return getProperty(PropertyKey.COMMENTS);
            case 11: return getProperty(PropertyKey.HEIGHT);
            case 12: return getProperty(PropertyKey.WIDTH);
            case 13: return getProperty(PropertyKey.BITRATE);
            default: return null;
        }
    }
}