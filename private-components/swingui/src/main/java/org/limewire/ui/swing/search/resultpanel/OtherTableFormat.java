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
    private static final int NUM_FILES_INDEX = 8;
    private static final int NUM_SOURCES_INDEX = 6;
    private static final int RELEVANCE_INDEX = 5;
    private static final int SIZE_INDEX = 3;
    
    public OtherTableFormat() {
        super(4, 4);

        columnNames = new String[] {
            "Name", "Extension", "Type", "Size", "",
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
            case 1: return fileExtension;
            case 2: return fileExtension; // TODO: RMV translate to verbal desc.
            case 3: return vsr.getSize();
            case 4: return vsr;
            case 5: return getProperty(PropertyKey.RELEVANCE);
            case 6: return vsr.getSources().size();
            case 7: return getProperty(PropertyKey.OWNER);
            case 8: return getProperty(PropertyKey.FILES_IN_ARCHIVE);
            default: return null;
        }
    }
}