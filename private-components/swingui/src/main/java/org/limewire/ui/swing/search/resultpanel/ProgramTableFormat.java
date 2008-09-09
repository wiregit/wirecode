package org.limewire.ui.swing.search.resultpanel;

import java.awt.Component;
import org.limewire.core.api.search.SearchResult.PropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ProgramTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private static final int ACTION_INDEX = 4;
    private static final int COMPANY_INDEX = 3;
    private static final int NAME_INDEX = 0;
    private static final int NUM_SOURCES_INDEX = 7;
    private static final int PLATFORM_INDEX = 2;
    private static final int RELEVANCE_INDEX = 5;
    private static final int SIZE_INDEX = 1;
    
    public ProgramTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX);

        columnNames = new String[] {
            "Name", "Size", "Platform", "Company", "",
            "Relevance", "Extension", "People with file", "Owner", "Author"
        };
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
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
            case NAME_INDEX: return getIconLabel(vsr);
            case SIZE_INDEX: return vsr.getSize();
            case PLATFORM_INDEX: return getProperty(PropertyKey.PLATFORM);
            case COMPANY_INDEX: return getProperty(PropertyKey.COMPANY);
            case ACTION_INDEX: return vsr;
            case RELEVANCE_INDEX: return getProperty(PropertyKey.RELEVANCE);
            case 6: return fileExtension;
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case 8: return getProperty(PropertyKey.OWNER);
            case 9: return getProperty(PropertyKey.AUTHOR);
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NAME_INDEX: return 360;
            case SIZE_INDEX: return 80;
            case PLATFORM_INDEX: return 80;
            case COMPANY_INDEX: return 120;
            case ACTION_INDEX: return 100;
            default: return 100;
        }
    }
}