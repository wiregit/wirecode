package org.limewire.ui.swing.library.table;

import java.util.Date;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
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
    public static final int FILE_COL = SIZE_COL + 1;
    public static final int TRACK_COL = FILE_COL + 1;
    public static final int MODIFIED_COL = TRACK_COL + 1;
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
        case FILE_COL:
            return I18n.tr("File Name");
        case TRACK_COL:
            return I18n.tr("Track");
        case MODIFIED_COL:
            return I18n.tr("Modified");
        case ACTION_COL:
            return I18n.tr("Sharing");
        }

        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public Object getColumnValue(FileItem baseObject, int column) {

        switch (column) {
        case PLAY_COL:
            return baseObject;
        case NAME_COL:
            return (baseObject.getProperty(FilePropertyKey.TITLE) == null) ? baseObject.getName() : (String)baseObject.getProperty(FilePropertyKey.TITLE);
        case ARTIST_COL:
            return baseObject.getProperty(FilePropertyKey.AUTHOR);
        case ALBUM_COL:
            return baseObject.getProperty(FilePropertyKey.ALBUM);
        case LENGTH_COL:
            return baseObject.getProperty(FilePropertyKey.LENGTH);
        case GENRE_COL:
            return baseObject.getProperty(FilePropertyKey.GENRE);
        case BITRATE_COL:
            return baseObject.getProperty(FilePropertyKey.BITRATE);
        case FILE_COL:
            return baseObject.getFileName();
        case SIZE_COL:
            return baseObject.getSize();
        case TRACK_COL:
            return baseObject.getProperty(FilePropertyKey.TRACK_NUMBER);
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
        return new int[] { MODIFIED_COL, TRACK_COL, FILE_COL, SIZE_COL, BITRATE_COL};
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
