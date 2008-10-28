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
public class OtherTableFormat extends ResultsTableFormat<VisualSearchResult> {

    private static final int ACTION_INDEX = 4;
    private static final int EXTENSION_INDEX = 1;
    private static final int NAME_INDEX = 0;
    private static final int NUM_FILES_INDEX = 8;
    private static final int NUM_SOURCES_INDEX = 6;
    private static final int RELEVANCE_INDEX = 5;
    private static final int SIZE_INDEX = 3;
    private static final int TYPE_INDEX = 2;
    
    public OtherTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Extension"), tr("Type"), tr("Size"), "",
                tr("Relevance"), tr("People with File"), tr("Owner"), tr("Number of Files"));
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
            case NAME_INDEX: return getIconLabel(vsr);
            case EXTENSION_INDEX: return fileExtension;
            case TYPE_INDEX: return fileExtension; // TODO: RMV translate to verbal desc.
            case SIZE_INDEX: return vsr.getSize();
            case ACTION_INDEX: return vsr;
            case RELEVANCE_INDEX: return getProperty(FilePropertyKey.RELEVANCE);
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case 7: return getProperty(FilePropertyKey.OWNER);
            case NUM_FILES_INDEX: return getProperty(FilePropertyKey.FILES_IN_ARCHIVE);
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NAME_INDEX: return 420;
            case EXTENSION_INDEX: return 80;
            case TYPE_INDEX: return 80;
            case SIZE_INDEX: return 60;
            case ACTION_INDEX: return 100;
            default: return 100;
        }
    }
}