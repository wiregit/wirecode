package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * video descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class VideoTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int NAME_INDEX = 0;
    public static final int EXTENSION_INDEX = 1;
    public static final int LENGTH_INDEX = 2;
    public static final int YEAR_INDEX = 3;
    public static final int QUALITY_INDEX = 4;
    public static final int ACTION_INDEX = 5;
    public static final int NUM_SOURCES_INDEX = 6;
    public static final int RATING_INDEX = 7;
    public static final int COMMENTS_INDEX = 8;
    public static final int HEIGHT_INDEX = 9;
    public static final int WIDTH_INDEX = 10;
    public static final int BITRATE_INDEX = 11;
    public static final int SIZE_INDEX = 12;

    public VideoTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Extension"), tr("Length"), tr("Year"), tr("Quality"), "",
                tr("People with File"), tr("Rating"),
                tr("Comments"), tr("Height"), tr("Width"), tr("Bitrate"), tr("Size"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == BITRATE_INDEX ? Integer.class :
            index == HEIGHT_INDEX ? Integer.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == RATING_INDEX ? Integer.class :
            index == WIDTH_INDEX ? Integer.class :
            index == YEAR_INDEX ? Integer.class :
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
            case ACTION_INDEX: return vsr;
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case RATING_INDEX: return getProperty(FilePropertyKey.RATING);
            case COMMENTS_INDEX: return getProperty(FilePropertyKey.COMMENTS);
            case HEIGHT_INDEX: return getProperty(FilePropertyKey.HEIGHT);
            case WIDTH_INDEX: return getProperty(FilePropertyKey.WIDTH);
            case BITRATE_INDEX: return getProperty(FilePropertyKey.BITRATE);
            case SIZE_INDEX: return vsr.getSize();
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NAME_INDEX: return 400;
            case EXTENSION_INDEX: return 60;
            case LENGTH_INDEX: return 60;
            case YEAR_INDEX: return 60;
            case QUALITY_INDEX: return 60;
            case ACTION_INDEX: return 100;
            default: return 100;
        }
    }
}