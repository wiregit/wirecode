package org.limewire.ui.swing.search.resultpanel.classic;

import java.awt.Component;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * This class specifies the content of a table that contains
 * music track descriptions.
 */
public class AudioTableFormat extends ResultsTableFormat<VisualSearchResult> {
    static final int FROM_INDEX = 0;
    static final int TITLE_INDEX = 1;
    static final int ARTIST_INDEX = 2;
    static final int ALBUM_INDEX = 3;
    public static final int LENGTH_INDEX = 4;
    public static final int QUALITY_INDEX = 5;
    static final int BITRATE_INDEX = 6;
    static final int GENRE_INDEX = 7;
    static final int TRACK_INDEX = 8;
    static final int YEAR_INDEX = 9;
    static final int NAME_INDEX = 10;
    static final int EXTENSION_INDEX = 11;
    public static final int SIZE_INDEX = 12;
    static final int DESCRIPTION_INDEX = 13;
    
    public AudioTableFormat() {
        super(TITLE_INDEX, FROM_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(FROM_INDEX, "CLASSIC_SEARCH_AUDIO_FROM", I18n.tr("From"), 55, true, true), 
                new ColumnStateInfo(TITLE_INDEX, "CLASSIC_SEARCH_AUDIO_TITLE", I18n.tr("Name"), 550, true, true),     
                new ColumnStateInfo(ARTIST_INDEX, "CLASSIC_SEARCH_AUDIO_ARTIST", I18n.tr("Artist"), 80, true, true), 
                new ColumnStateInfo(ALBUM_INDEX, "CLASSIC_SEARCH_AUDIO_ALBUM", I18n.tr("Album"), 60, true, true), 
                new ColumnStateInfo(LENGTH_INDEX, "CLASSIC_SEARCH_AUDIO_LENGTH", I18n.tr("Length"), 60, true, true), 
                new ColumnStateInfo(QUALITY_INDEX, "CLASSIC_SEARCH_AUDIO_QUALITY", I18n.tr("Quality"), 55, true, true), 
                new ColumnStateInfo(BITRATE_INDEX, "CLASSIC_SEARCH_AUDIO_BITRATE", I18n.tr("Bitrate"), 55, false, true), 
                new ColumnStateInfo(GENRE_INDEX, "CLASSIC_SEARCH_AUDIO_GENRE", I18n.tr("Genre"), 80, false, true),
                new ColumnStateInfo(TRACK_INDEX, "CLASSIC_SEARCH_AUDIO_TRACK", I18n.tr("Track"), 60, false, true), 
                new ColumnStateInfo(YEAR_INDEX, "CLASSIC_SEARCH_AUDIO_YEAR", I18n.tr("Year"), 60, false, true), 
                new ColumnStateInfo(NAME_INDEX, "CLASSIC_SEARCH_AUDIO_NAME", I18n.tr("Filename"), 550, false, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "CLASSIC_SEARCH_AUDIO_EXTENSION", I18n.tr("Extension"), 60, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "CLASSIC_SEARCH_AUDIO_SIZE", I18n.tr("Size"), 80, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "CLASSIC_SEARCH_AUDIO_DESCRIPTION", I18n.tr("Description"), 60, false, true)
        });
    }
    
    @Override
    public Class getColumnClass(int column) {
        switch(column) {
        case TITLE_INDEX: return Component.class;
        case BITRATE_INDEX: return Integer.class;
        case TRACK_INDEX: return Integer.class;
        case FROM_INDEX: return VisualSearchResult.class;
        }
        return super.getColumnClass(column);
    }
    
    public Object getColumnValue(VisualSearchResult vsr, int column) {
        switch(column) {
            case FROM_INDEX: return vsr;
            case TITLE_INDEX: return vsr;
            case ARTIST_INDEX: return vsr.getProperty(FilePropertyKey.AUTHOR);
            case ALBUM_INDEX: return vsr.getProperty(FilePropertyKey.ALBUM);
            case LENGTH_INDEX: return vsr.getProperty(FilePropertyKey.LENGTH);
            case QUALITY_INDEX: return vsr;
            case BITRATE_INDEX: return vsr.getProperty(FilePropertyKey.BITRATE);
            case GENRE_INDEX: return vsr.getProperty(FilePropertyKey.GENRE);
            case TRACK_INDEX: return vsr.getProperty(FilePropertyKey.TRACK_NUMBER);
            case YEAR_INDEX: return vsr.getProperty(FilePropertyKey.YEAR);
            case NAME_INDEX: return vsr.getProperty(FilePropertyKey.NAME);
            case EXTENSION_INDEX: return vsr.getFileExtension();
            case SIZE_INDEX: return vsr.getSize();
            case DESCRIPTION_INDEX: return "";
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
}