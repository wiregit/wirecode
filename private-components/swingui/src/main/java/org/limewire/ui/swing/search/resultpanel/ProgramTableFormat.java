package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;

import org.limewire.core.api.FilePropertyKey;
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
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Size"), tr("Platform"), tr("Company"), "",
                tr("Relevance"), tr("Extension"), tr("People with File"), tr("Owner"),
                tr("Author"));
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
            case PLATFORM_INDEX: return getProperty(FilePropertyKey.PLATFORM);
            case COMPANY_INDEX: return getProperty(FilePropertyKey.COMPANY);
            case ACTION_INDEX: return vsr;
            case RELEVANCE_INDEX: return getProperty(FilePropertyKey.RELEVANCE);
            case 6: return fileExtension;
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case 8: return getProperty(FilePropertyKey.OWNER);
            case 9: return getProperty(FilePropertyKey.AUTHOR);
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