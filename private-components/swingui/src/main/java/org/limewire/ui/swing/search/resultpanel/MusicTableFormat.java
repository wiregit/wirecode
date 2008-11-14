package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a table that contains
 * music track descriptions.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class MusicTableFormat extends ResultsTableFormat<VisualSearchResult> {

    public static final int NAME_INDEX = 0;
    public static final int ARTIST_INDEX = 1;
    public static final int ALBUM_INDEX = 2;
    public static final int LENGTH_INDEX = 3;
    public static final int QUALITY_INDEX = 4;
    public static final int ACTION_INDEX = 5;
    public static final int BITRATE_INDEX = 6;
    public static final int GENRE_INDEX = 7;
    public static final int TRACK_INDEX = 8;
    public static final int NUM_SOURCES_INDEX = 9;
    public static final int FILE_EXTENSION_INDEX = 10;
    public static final int SIZE_INDEX = 11;

    
    public MusicTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Artist"), tr("Album"), tr("Length"), tr("Quality"),
                "", tr("Bitrate"), tr("Genre"), tr("Track"),
                tr("People with file"), tr("Extension"), tr("Size"));
    }

    @Override
    public Class getColumnClass(int index) {
        Class clazz =
            index == BITRATE_INDEX ? Integer.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == TRACK_INDEX ? Integer.class :
            super.getColumnClass(index);
        return clazz;
    }

    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;

        switch (index) {
            case NAME_INDEX: return getProperty(FilePropertyKey.NAME);
            case ARTIST_INDEX: return getProperty(FilePropertyKey.AUTHOR);
            case ALBUM_INDEX: return getProperty(FilePropertyKey.ALBUM);
            case LENGTH_INDEX: return getProperty(FilePropertyKey.LENGTH);
            case QUALITY_INDEX: return getProperty(FilePropertyKey.QUALITY);
            case ACTION_INDEX: return vsr;
            case BITRATE_INDEX: return getProperty(FilePropertyKey.BITRATE);
            case GENRE_INDEX: return getProperty(FilePropertyKey.GENRE);
            case TRACK_INDEX: return getProperty(FilePropertyKey.TRACK_NUMBER);
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case FILE_EXTENSION_INDEX: return vsr.getFileExtension();
            case SIZE_INDEX: return vsr.getSize();
            default: return null;
        }
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch (index) {
            case NAME_INDEX: return 200;
            case ARTIST_INDEX: return 155;
            case ALBUM_INDEX: return 155;
            case LENGTH_INDEX: return 50;
            case QUALITY_INDEX: return 80;
            case ACTION_INDEX: return 100;
            default: return 100;
        }
    }
}