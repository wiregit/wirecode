package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Audio Table for LW buddies and Browse hosts
 */
public class RemoteAudioTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    public static final int NAME_COL =0;
    public static final int ARTIST_COL = NAME_COL + 1;
    public static final int ALBUM_COL = ARTIST_COL + 1;
    public static final int LENGTH_COL = ALBUM_COL + 1;
    public static final int QUALITY_COL = LENGTH_COL + 1;
    public static final int GENRE_COL = QUALITY_COL + 1;
    public static final int BITRATE_COL = GENRE_COL + 1;
    public static final int SIZE_COL = BITRATE_COL + 1;
    public static final int TRACK_COL = SIZE_COL + 1;
    public static final int YEAR_COL = TRACK_COL + 1;
    public static final int FILENAME_COL = YEAR_COL + 1;
    public static final int EXTENSION_COL = FILENAME_COL + 1;
    public static final int DESCRIPTION_COL = EXTENSION_COL + 1;
    public static final int COLUMN_COUNT = DESCRIPTION_COL + 1;


    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {
        switch (column) {
            case NAME_COL:
                return I18n.tr("Name");
            case ARTIST_COL:
                return I18n.tr("Artist");
            case ALBUM_COL:
                return I18n.tr("Album");
            case LENGTH_COL:
                return I18n.tr("Length");
            case QUALITY_COL:
                return I18n.tr("Quality");
            case GENRE_COL:
                return I18n.tr("Genre");
            case BITRATE_COL:
                return I18n.tr("Bitrate");
            case SIZE_COL:
                return I18n.tr("Size");
            case TRACK_COL:
                return I18n.tr("Track");
            case YEAR_COL:
                return I18n.tr("Year");
            case FILENAME_COL:
                return I18n.tr("Filename");
            case EXTENSION_COL:
                return I18n.tr("Extension");
            case DESCRIPTION_COL:
                return I18n.tr("Description");
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch (column) {
            case NAME_COL:
                return (baseObject.getProperty(FilePropertyKey.TITLE) == null) ? baseObject.getProperty(FilePropertyKey.NAME) : baseObject.getProperty(FilePropertyKey.TITLE);
            case ARTIST_COL:
                return baseObject.getProperty(FilePropertyKey.AUTHOR);
            case ALBUM_COL:
                return baseObject.getProperty(FilePropertyKey.ALBUM);
            case LENGTH_COL:
                return baseObject.getProperty(FilePropertyKey.LENGTH);
            case QUALITY_COL:
                return "";
            case GENRE_COL:
                return baseObject.getProperty(FilePropertyKey.GENRE);
            case BITRATE_COL:
                return baseObject.getProperty(FilePropertyKey.BITRATE);
            case SIZE_COL:
                return baseObject.getSize();
            case TRACK_COL:
                return baseObject.getProperty(FilePropertyKey.TRACK_NUMBER);
            case YEAR_COL:
                return baseObject.getProperty(FilePropertyKey.YEAR);
            case FILENAME_COL:
                return baseObject.getProperty(FilePropertyKey.NAME);
            case EXTENSION_COL:
                return FileUtils.getFileExtension(baseObject.getFileName());
            case DESCRIPTION_COL:
                return baseObject.getProperty(FilePropertyKey.COMMENTS);
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return -1;
    }

    @Override
    public int[] getDefaultHiddenColums() {
        return new int[] { DESCRIPTION_COL, EXTENSION_COL, FILENAME_COL, YEAR_COL, TRACK_COL, SIZE_COL, BITRATE_COL, GENRE_COL};
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return false;
    }
}
