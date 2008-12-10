package org.limewire.ui.swing.search.resultpanel.classic;

import static org.limewire.ui.swing.util.I18n.tr;

import java.util.Calendar;
import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;

/**
 * This class specifies the content of a table that contains
 * image descriptions.
 */
public class ImageTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int FROM_INDEX = 0;
    public static final int NAME_INDEX = 1;
    public static final int FILE_EXTENSION_INDEX = 2;
    public static final int DATE_INDEX = 3;
    public static final int SIZE_INDEX = 4;
    public static final int DESCRIPTION_INDEX = 5;
    public static final int TITLE_INDEX = 6;

    public ImageTableFormat() {
        super(DATE_INDEX,
                tr("From"),
                tr("Filename"), 
                tr("Extension"), 
                tr("Date created"), 
                tr("Size"),
                tr("Description"),
                tr("Title"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == DATE_INDEX ? Calendar.class :
            index == FROM_INDEX ? VisualSearchResult.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        String fileExtension = vsr.getFileExtension();

        switch (index) {
            case NAME_INDEX: return getProperty(FilePropertyKey.NAME);
            case FILE_EXTENSION_INDEX: return fileExtension;
            case DATE_INDEX: return getProperty(FilePropertyKey.DATE_CREATED);
            case FROM_INDEX: return vsr;
            case SIZE_INDEX: return vsr.getSize();
            case DESCRIPTION_INDEX: return "";
            case TITLE_INDEX: return getProperty(FilePropertyKey.TITLE);
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case FROM_INDEX: return 55;
            case NAME_INDEX: return 540;
            case FILE_EXTENSION_INDEX: return 60;
            case DATE_INDEX: return 100;
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