package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * music track descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class MusicTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public MusicTableFormat() {
        columnNames = new String[] {
            "Title", "Artist", "Album", "Length", "Quality",
            "Actions", "Bitrate", "Genre", "Track", "Relevance",
            "People with File", "Owner", "Type", "Sample Rate"
        };

        actionColumnIndex = 5;
    }

    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case 0: return getProperty(PropertyKey.NAME);
            case 1: return getProperty(PropertyKey.ARTIST_NAME);
            case 2: return getProperty(PropertyKey.ALBUM_TITLE);
            case 3: return getProperty(PropertyKey.LENGTH);
            case 4: return getProperty(PropertyKey.QUALITY);
            case 5: return vsr;
            case 6: return getProperty(PropertyKey.BITRATE);
            case 7: return getProperty(PropertyKey.GENRE);
            case 8: return getProperty(PropertyKey.TRACK_NUMBER);
            case 9: return getProperty(PropertyKey.RELEVANCE);
            case 10: return ""; // people with file
            case 11: return getProperty(PropertyKey.OWNER);
            case 12: return vsr.getFileExtension();
            case 13: return getProperty(PropertyKey.SAMPLE_RATE);
            default: return null;
        }
    }
}