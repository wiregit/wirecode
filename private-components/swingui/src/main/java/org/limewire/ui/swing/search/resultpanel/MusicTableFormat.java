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
            case 0: return get(PropertyKey.NAME);
            case 1: return get(PropertyKey.ARTIST_NAME);
            case 2: return get(PropertyKey.ALBUM_TITLE);
            case 3: return get(PropertyKey.LENGTH);
            case 4: return get(PropertyKey.QUALITY);
            case 5: return vsr;
            case 6: return get(PropertyKey.BITRATE);
            case 7: return get(PropertyKey.GENRE);
            case 8: return get(PropertyKey.TRACK_NUMBER);
            case 9: return get(PropertyKey.RELEVANCE);
            case 10: return ""; // people with file
            case 11: return ""; // owner
            case 12: return vsr.getFileExtension();
            case 13: return get(PropertyKey.SAMPLE_RATE);
            default: return null;
        }
    }
}