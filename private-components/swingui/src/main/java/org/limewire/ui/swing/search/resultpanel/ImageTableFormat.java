package org.limewire.ui.swing.search.resultpanel;

import java.util.Calendar;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * image descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ImageTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private static final int DATE_CREATED_INDEX = 2;

    public ImageTableFormat() {
        columnNames = new String[] {
            "Name", "Type", "Date Created", "Actions", "Relevance",
            "People with File", "Owner"
        };

        actionColumnIndex = 3;
    }

    @Override
    public Class getColumnClass(int index) {
        return index == DATE_CREATED_INDEX ? Calendar.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String fileExtension = vsr.getFileExtension();

        switch (index) {
            case 0: return getProperty(PropertyKey.NAME);
            case 1: return fileExtension; // TODO: RMV improve
            case 2: return getProperty(PropertyKey.DATE_CREATED);
            case 3: return vsr;
            case 4: return getProperty(PropertyKey.RELEVANCE);
            case 5: return ""; // people with file
            case 6: return getProperty(PropertyKey.OWNER);
            default: return null;
        }
    }
}