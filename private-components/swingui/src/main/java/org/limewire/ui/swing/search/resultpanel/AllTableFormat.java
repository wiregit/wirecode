package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * objects representing any kind of media.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class AllTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int NAME_INDEX = 0;
    public static final int TYPE_INDEX = 1;
    public static final int SIZE_INDEX = 2;
    public static final int ACTION_INDEX = 3;
    public static final int EXTENSION_INDEX = 4;
    public static final int NUM_SOURCES_INDEX = 5;
    
    public AllTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX, tr("Name"), tr("Type"), tr("Size"), "",
                 tr("Extension"), tr("People with file"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == SIZE_INDEX ? Long.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case NAME_INDEX: return getIconLabel(vsr);
            case TYPE_INDEX: return vsr.getCategory();
            case SIZE_INDEX: return vsr.getSize();
            case ACTION_INDEX: return vsr;
            case EXTENSION_INDEX: return vsr.getFileExtension();
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NAME_INDEX: return 500;
            case TYPE_INDEX: return 80;
            case SIZE_INDEX: return 60;
            case ACTION_INDEX: return 100;
            default: return 100;
        }
    }
}