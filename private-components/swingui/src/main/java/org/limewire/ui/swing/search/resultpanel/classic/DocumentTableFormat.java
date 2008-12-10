package org.limewire.ui.swing.search.resultpanel.classic;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.util.Calendar;
import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;

/**
 * This class specifies the content of a table that contains
 * document descriptions.
 */
public class DocumentTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int FROM_INDEX = 0;
    public static final int NAME_INDEX = 1;
    public static final int TYPE_INDEX = 2;
    public static final int SIZE_INDEX = 3;
    public static final int DATE_INDEX = 4;
    public static final int FILE_EXTENSION_INDEX = 5;
    public static final int AUTHOR_INDEX = 6;
    
    public DocumentTableFormat() {
        super(FILE_EXTENSION_INDEX,
                tr("From"),
                tr("Filename"), 
                tr("Type"), 
                tr("Size"), 
                tr("Date created"), 
                tr("Extension"), 
                tr("Author"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == NAME_INDEX ? Component.class :
            index == DATE_INDEX ? Calendar.class :
            index == SIZE_INDEX ? Integer.class :
            index == FROM_INDEX ? VisualSearchResult.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String fileExtension = vsr.getFileExtension();

        switch (index) {
            case NAME_INDEX: return vsr;
            case TYPE_INDEX: return fileExtension; // TODO: RMV improve
            case SIZE_INDEX: return vsr.getSize();
            case DATE_INDEX: return getProperty(FilePropertyKey.DATE_CREATED);
            case FROM_INDEX: return vsr;
            case FILE_EXTENSION_INDEX: return fileExtension;
            case AUTHOR_INDEX: return getProperty(FilePropertyKey.AUTHOR);
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case FROM_INDEX: return 55;
            case NAME_INDEX: return 360;
            case TYPE_INDEX: return 80;
            case SIZE_INDEX: return 80;
            case DATE_INDEX: return 100;
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