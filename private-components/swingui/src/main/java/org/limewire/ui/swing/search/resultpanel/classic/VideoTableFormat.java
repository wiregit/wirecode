package org.limewire.ui.swing.search.resultpanel.classic;

import static org.limewire.ui.swing.util.I18n.tr;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;

/**
 * This class specifies the content of a table that contains
 * video descriptions.
 */
public class VideoTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int FROM_INDEX = 0;
    public static final int NAME_INDEX = 1;
    public static final int EXTENSION_INDEX = 2;
    public static final int LENGTH_INDEX = 3;
    public static final int MISC_INDEX = 4;
    public static final int QUALITY_INDEX = 5;
    public static final int SIZE_INDEX = 6;
    public static final int RATING_INDEX = 7;
    public static final int DIMENSION_INDEX = 8;
    public static final int YEAR_INDEX = 9;
    public static final int DESCRIPTION_INDEX = 10;

    public VideoTableFormat() {
        super(SIZE_INDEX,
                tr("From"), 
                tr("Filename"), 
                tr("Extension"), 
                tr("Length"), 
                tr("Misc"),
                tr("Quality"), 
                tr("Size"),
                tr("Rating"),
                tr("Resolution"), 
                tr("Year"),
                tr("Description"));
    }

    @Override
    public Class getColumnClass(int index) {
        return 
            index == RATING_INDEX ? Integer.class :
            index == YEAR_INDEX ? Integer.class :
            index == FROM_INDEX ? VisualSearchResult.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;
        
        String fileExtension = vsr.getFileExtension();

        switch (index) {
            case NAME_INDEX: return getProperty(FilePropertyKey.NAME);
            case EXTENSION_INDEX: return fileExtension;
            case LENGTH_INDEX: return getProperty(FilePropertyKey.LENGTH);
            case YEAR_INDEX: return getProperty(FilePropertyKey.YEAR);
            case QUALITY_INDEX: return getProperty(FilePropertyKey.QUALITY);
            case MISC_INDEX: return getProperty(FilePropertyKey.COMMENTS);
            case DESCRIPTION_INDEX: return "";
            case FROM_INDEX: return vsr;
            case RATING_INDEX: return getProperty(FilePropertyKey.RATING);
            case DIMENSION_INDEX:
                if(getProperty(FilePropertyKey.WIDTH) == null || getProperty(FilePropertyKey.HEIGHT) == null)
                    return null;
                else
                    return (getProperty(FilePropertyKey.WIDTH) + " X " + getProperty(FilePropertyKey.HEIGHT));
            case SIZE_INDEX: return vsr.getSize();
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case FROM_INDEX: return 55;
            case NAME_INDEX: return 410;
            case EXTENSION_INDEX: return 60;
            case LENGTH_INDEX: return 60;
            case YEAR_INDEX: return 60;
            case SIZE_INDEX: return 60;
            case QUALITY_INDEX: return 60;
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