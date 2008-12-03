package org.limewire.ui.swing.search.resultpanel.classic;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.util.Comparator;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;

/**
 * This class specifies the content of a table that contains
 * objects representing any kind of media.
 */
public class AllTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int FROM_INDEX = 0;
    public static final int NAME_INDEX = 1;
    public static final int TYPE_INDEX = 2;
    public static final int SIZE_INDEX = 3;
    public static final int FILE_EXTENSION_INDEX = 4;
    
    public AllTableFormat() {
        super(FILE_EXTENSION_INDEX, 
              tr("From"),
              tr("Name"), 
              tr("Type"), 
              tr("Size"), 
              tr("Extension"));
    }
    
    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == SIZE_INDEX ? Long.class :
            index == FROM_INDEX ? VisualSearchResult.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case FROM_INDEX: return vsr;
            case NAME_INDEX: return vsr;
            case TYPE_INDEX: return vsr.getCategory();
            case SIZE_INDEX: return vsr.getSize();
            case FILE_EXTENSION_INDEX: return vsr.getFileExtension();
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case FROM_INDEX: return 100;
            case NAME_INDEX: return 500;
            case TYPE_INDEX: return 80;
            case SIZE_INDEX: return 60;
            case FILE_EXTENSION_INDEX: return 60;
            default: return 100;
        }
    }

    @Override
    public boolean isEditable(VisualSearchResult vsr, int column) {
        return column == FROM_INDEX;
    }

    @Override
    public int getNameColumn() {
        return NAME_INDEX;
    }

    /**
     * If the FromColumn is sorted, use a custom column sorter
     * otherwise it is assumed the column returns a value that 
     * implements the Comparable interface
     */
    @Override
    public Comparator getColumnComparator(int index) {
        switch (index) {
            case FROM_INDEX:
                return getFromComparator();
        }
        return super.getColumnComparator(index);
    }
}