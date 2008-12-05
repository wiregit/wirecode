package org.limewire.ui.swing.search.resultpanel.classic;

import static org.limewire.ui.swing.util.I18n.tr;

import java.util.Comparator;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;

/**
 * This class specifies the content of a table that contains
 * music track descriptions.
 */
public class AudioTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int FROM_INDEX = 0;
    public static final int TITLE_INDEX = 1;
    public static final int ARTIST_INDEX = 2;
    public static final int ALBUM_INDEX = 3;
    public static final int LENGTH_INDEX = 4;
    public static final int QUALITY_INDEX = 5;
    public static final int BITRATE_INDEX = 6;
    public static final int GENRE_INDEX = 7;
    public static final int TRACK_INDEX = 8;
    public static final int YEAR_INDEX = 9;
    public static final int NAME_INDEX = 10;
    public static final int FILE_EXTENSION_INDEX = 11;
    public static final int SIZE_INDEX = 12;

    
    public AudioTableFormat() {
        super(QUALITY_INDEX,
                tr("From"),
                tr("Title"),
                tr("Artist"), 
                tr("Album"), 
                tr("Length"), 
                tr("Quality"),
                tr("Bitrate"), 
                tr("Genre"), 
                tr("Track"), 
                tr("Year"),
                tr("Name"), 
                tr("Extension"), 
                tr("Size"));
    }
    
    @Override
    public Class getColumnClass(int index) {
        Class clazz =
            index == BITRATE_INDEX ? Integer.class :
            index == TRACK_INDEX ? Integer.class :
            index == FROM_INDEX ? VisualSearchResult.class :
            super.getColumnClass(index);
        return clazz;
    }

    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;
System.out.println("qualitgy " + getProperty(FilePropertyKey.QUALITY));
        switch (index) {
            case FROM_INDEX: return vsr;
            case TITLE_INDEX: return (getProperty(FilePropertyKey.TITLE) == null) ? getProperty(FilePropertyKey.NAME) : getProperty(FilePropertyKey.TITLE);
            case ARTIST_INDEX: return getProperty(FilePropertyKey.AUTHOR);
            case ALBUM_INDEX: return getProperty(FilePropertyKey.ALBUM);
            case LENGTH_INDEX: return getProperty(FilePropertyKey.LENGTH);
            case QUALITY_INDEX: return getProperty(FilePropertyKey.QUALITY);
            case BITRATE_INDEX: return getProperty(FilePropertyKey.BITRATE);
            case GENRE_INDEX: return getProperty(FilePropertyKey.GENRE);
            case TRACK_INDEX: return getProperty(FilePropertyKey.TRACK_NUMBER);
            case YEAR_INDEX: return getProperty(FilePropertyKey.YEAR);
            case NAME_INDEX: return getProperty(FilePropertyKey.NAME);
            case FILE_EXTENSION_INDEX: return vsr.getFileExtension();
            case SIZE_INDEX: return vsr.getSize();
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case FROM_INDEX: return 100;
            case TITLE_INDEX: return 200;
            case ARTIST_INDEX: return 155;
            case ALBUM_INDEX: return 155;
            case LENGTH_INDEX: return 50;
            case QUALITY_INDEX: return 80;
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