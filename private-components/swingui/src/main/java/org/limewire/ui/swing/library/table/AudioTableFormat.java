package org.limewire.ui.swing.library.table;

import java.util.Date;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileItem.Keys;
import org.limewire.ui.swing.util.I18n;

public class AudioTableFormat<T extends FileItem> implements LibraryTableFormat<T> {

    public static final int PLAY_COL = 0;
    public static final int NAME_COL = PLAY_COL + 1;
    public static final int ARTIST_COL = NAME_COL + 1;
    public static final int ALBUM_COL = ARTIST_COL + 1;
    public static final int LENGTH_COL = ALBUM_COL + 1;
    public static final int GENRE_COL = LENGTH_COL + 1;
    public static final int BITRATE_COL = GENRE_COL + 1;
    public static final int SIZE_COL = BITRATE_COL + 1;
    public static final int TRACK_COL = SIZE_COL + 1;
    public static final int SAMPLE_RATE_COL = TRACK_COL + 1;
    public static final int MODIFIED_COL = SAMPLE_RATE_COL + 1;
    public static final int ACTION_COL = MODIFIED_COL + 1;
    public static final int COLUMN_COUNT = ACTION_COL + 1;


    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {

        switch (column) {
        case PLAY_COL:
            return I18n.tr("Play");
        case NAME_COL:
            return I18n.tr("Name");
        case ARTIST_COL:
            return I18n.tr("Artist");
        case ALBUM_COL:
            return I18n.tr("Album");
        case LENGTH_COL:
            return I18n.tr("Length");
        case GENRE_COL:
            return I18n.tr("Genre");
        case BITRATE_COL:
            return I18n.tr("Bitrate");
        case SIZE_COL:
            return I18n.tr("Size");
        case TRACK_COL:
            return I18n.tr("Track");
        case SAMPLE_RATE_COL:
            return I18n.tr("Sample Rate");
        case MODIFIED_COL:
            return I18n.tr("Modified");
        case ACTION_COL:
            return I18n.tr("Share");
        }

        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public Object getColumnValue(FileItem baseObject, int column) {

        switch (column) {
        case PLAY_COL:
            return baseObject;
        case NAME_COL:
            return (baseObject.getProperty(Keys.TITLE) == null) ? baseObject.getName() : (String)baseObject.getProperty(Keys.TITLE);
        case ARTIST_COL:
            return baseObject.getProperty(Keys.AUTHOR);
        case ALBUM_COL:
            return baseObject.getProperty(Keys.ALBUM);
        case LENGTH_COL:
            return baseObject.getProperty(Keys.LENGTH);
        case GENRE_COL:
            return baseObject.getProperty(Keys.GENRE);
        case BITRATE_COL:
            return baseObject.getProperty(Keys.BITRATE);
        case SIZE_COL:
            return baseObject.getSize();
        case TRACK_COL:
            return baseObject.getProperty(Keys.TRACK);
        case SAMPLE_RATE_COL:
            return baseObject.getProperty(Keys.SAMPLE_RATE);
        case MODIFIED_COL:
            return new Date(baseObject.getLastModifiedTime());
        case ACTION_COL:
            return baseObject;
        }

        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return ACTION_COL;
    }

    @Override
    public int[] getDefaultHiddenColums() {
        return new int[] { MODIFIED_COL, SAMPLE_RATE_COL, TRACK_COL, SIZE_COL, BITRATE_COL};
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return column == PLAY_COL || column == ACTION_COL;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }

 
}
