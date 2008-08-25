package org.limewire.ui.swing.search.resultpanel;

import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * objects representing any kind of media.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class AllTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public AllTableFormat() {
        columnNames = new String[] {
            "Icon", "Name", "Type", "Size", "Actions",
            "Relevance", "People with File", "Owner"
        };

        actionColumnIndex = 4;
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String type = vsr.getFileExtension();
        // TODO: RMV How can this obtain an icon for the media type?
        //Icon icon = getIcon(type);

        switch (index) {
            case 0: return "icon?"; // icon;
            case 1: return get(PropertyKey.NAME);
            case 2: return type;
            case 3: return vsr.getSize();
            case 4: return vsr;
            case 5: return get(PropertyKey.RELEVANCE);
            case 6: return ""; // people with file
            case 7: return ""; // owner
            default: return null;
        }
    }
}