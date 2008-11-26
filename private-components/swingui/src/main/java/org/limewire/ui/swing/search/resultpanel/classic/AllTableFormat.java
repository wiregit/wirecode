package org.limewire.ui.swing.search.resultpanel.classic;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;

/**
 * This class specifies the content of a table that contains
 * objects representing any kind of media.
 */
public class AllTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int NUM_SOURCES_INDEX = 0;
    public static final int NAME_INDEX = 1;
    public static final int TYPE_INDEX = 2;
    public static final int SIZE_INDEX = 3;
    public static final int FROM_INDEX = 4;
    public static final int EXTENSION_INDEX = 5;
    
    public AllTableFormat() {
        super(FROM_INDEX, tr("People with file"),
              tr("Name"), 
              tr("Type"), 
              tr("Size"), 
              tr("From"),
              tr("Extension"));
    }
    
    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == SIZE_INDEX ? Long.class :
            index == FROM_INDEX ? VisualSearchResult.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case NAME_INDEX: return getIconLabel(vsr);
            case TYPE_INDEX: return vsr.getCategory();
            case SIZE_INDEX: return vsr.getSize();
            case FROM_INDEX: return vsr;
            case EXTENSION_INDEX: return vsr.getFileExtension();
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NUM_SOURCES_INDEX: return 100;
            case NAME_INDEX: return 500;
            case TYPE_INDEX: return 80;
            case SIZE_INDEX: return 60;
            case FROM_INDEX: return 200;
            default: return 100;
        }
    }

    @Override
    public boolean isEditable(VisualSearchResult vsr, int column) {
        return column == FROM_INDEX;
    }
}