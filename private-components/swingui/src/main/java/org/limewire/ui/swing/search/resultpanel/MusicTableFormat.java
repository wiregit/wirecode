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

    private static final int ACTION_INDEX = 5;
    private static final int ALBUM_INDEX = 2;
    private static final int ARTIST_INDEX = 1;
    private static final int BITRATE_INDEX = 6;
    private static final int LENGTH_INDEX = 3;
    private static final int NAME_INDEX = 0;
    private static final int NUM_SOURCES_INDEX = 10;
    private static final int QUALITY_INDEX = 4;
    private static final int RELEVANCE_INDEX = 9;
    private static final int TRACK_INDEX = 8;

    public MusicTableFormat() {
        super(ACTION_INDEX, ACTION_INDEX,
                tr("Name"), tr("Artist"), tr("Album"), tr("Length"), tr("Quality"),
                "", tr("Bitrate"), tr("Genre"), tr("Track"), tr("Relevance"),
                tr("People with file"), tr("Extension"), tr("Sample Rate"));
    }

    @Override
    public Class getColumnClass(int index) {
        Class clazz =
            index == BITRATE_INDEX ? Integer.class :
            index == NUM_SOURCES_INDEX ? Integer.class :
            index == RELEVANCE_INDEX ? Integer.class :
            index == TRACK_INDEX ? Integer.class :
            super.getColumnClass(index);
        //System.out.println("MusicTableFormat: index = " + index);
        //System.out.println("MusicTableFormat: clazz = " + clazz.getName());
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
            case 7: return getProperty(FilePropertyKey.GENRE);
            case TRACK_INDEX: return getProperty(FilePropertyKey.TRACK_NUMBER);
            case RELEVANCE_INDEX: return vsr.getRelevance();
            case NUM_SOURCES_INDEX: return vsr.getSources().size();
            case 11: return vsr.getFileExtension();
            case 12: return getProperty(FilePropertyKey.SAMPLE_RATE);
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