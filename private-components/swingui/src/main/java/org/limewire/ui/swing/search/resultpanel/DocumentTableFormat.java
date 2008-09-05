package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import java.util.Calendar;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class DocumentTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private static final int DATE_CREATED_INDEX = 3;
    private static final int NAME_INDEX = 0;
    private static final int NUM_SOURCES_INDEX = 6;
    private static final int RELEVANCE_INDEX = 5;
    private static final int SIZE_INDEX = 2;

    public DocumentTableFormat() {
        super(4, 4);

        columnNames = new String[] {
            "Name", "Type", "Size", "Date Created",
            "", "Relevance", "People with File", "Owner", "Author"
        };
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == DATE_CREATED_INDEX ? Calendar.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == RELEVANCE_INDEX ? Integer.class :
            index == SIZE_INDEX ? Integer.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String fileExtension = vsr.getFileExtension();

        switch (index) {
            case 0: return getIconLabel(vsr);
            case 1: return fileExtension; // TODO: RMV improve
            case 2: return vsr.getSize();
            case 3: return getProperty(PropertyKey.DATE_CREATED);
            case 4: return vsr;
            case 5: return getProperty(PropertyKey.RELEVANCE);
            case 6: return vsr.getSources().size();
            case 7: return getProperty(PropertyKey.OWNER);
            case 8: return getProperty(PropertyKey.AUTHOR);
            default: return null;
        }
    }
}