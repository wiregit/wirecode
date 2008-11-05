package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;

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
    private static final int NUM_SOURCES_INDEX = 5;
    private static final int SIZE_INDEX = 3;
    private static final int TYPE_INDEX = 2;
    
    public OtherTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Extension"), tr("Type"), tr("Size"), "",
                tr("People with File"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
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
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
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