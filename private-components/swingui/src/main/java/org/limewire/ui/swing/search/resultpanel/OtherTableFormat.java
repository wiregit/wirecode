package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class OtherTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private static final int NAME_INDEX = 0;
    private static final int NUM_FILES_INDEX = 7;
    private static final int NUM_SOURCES_INDEX = 5;
    private static final int RELEVANCE_INDEX = 4;
    private static final int SIZE_INDEX = 2;
    
    public OtherTableFormat() {
        super(3, 3);

        columnNames = new String[] {
            "Name", "Type", "Size", "",
            "Relevance", "People with File", "Owner", "Number of Files"
        };
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == NUM_FILES_INDEX ? Integer.class :
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
            case 3: return vsr;
            case 4: return getProperty(PropertyKey.RELEVANCE);
            case 5: return vsr.getSources().size();
            case 6: return getProperty(PropertyKey.OWNER);
            case 7: return getProperty(PropertyKey.FILES_IN_ARCHIVE);
            default: return null;
        }
    }
}