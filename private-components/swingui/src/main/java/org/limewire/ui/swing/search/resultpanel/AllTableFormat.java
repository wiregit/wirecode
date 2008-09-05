package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.util.MediaType;

/**
 * This class specifies the content of a table that contains
 * objects representing any kind of media.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class AllTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private static final int NAME_INDEX = 0;
    private static final int NUM_SOURCES_INDEX = 5;
    private static final int RELEVANCE_INDEX = 4;
    private static final int SIZE_INDEX = 2;

    public AllTableFormat() {
        super(3, 3);

        columnNames = new String[] {
            "Name", "Type", "Size", "",
            "Relevance", "People with File", "Owner"
        };
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == RELEVANCE_INDEX ? Integer.class :
            index == SIZE_INDEX ? Long.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case 0: return getIconLabel(vsr);
            case 1: return getMediaType(vsr);
            case 2: return vsr.getSize();
            case 3: return vsr;
            case 4: return getProperty(PropertyKey.RELEVANCE);
            case 5: return vsr.getSources().size();
            case 6: return getProperty(PropertyKey.OWNER);
            default: return null;
        }
    }

    public static String getMediaType(VisualSearchResult vsr) {
        String ext = vsr.getFileExtension();
        MediaType mediaType = MediaType.getMediaTypeForExtension(ext);
        // TODO: RMV improve the text returned
        return mediaType == null ? ext : mediaType.toString();
    }
}