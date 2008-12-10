package org.limewire.ui.swing.library.table;

import java.util.Comparator;
import java.util.Date;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PropertyUtils;
import org.limewire.util.FileUtils;

/**
 * Table format for the Audio Table when it is in My Library
 */
public class AudioTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    public static final int PLAY_COL = 0;
    public static final int TITLE_COL = PLAY_COL + 1;
    public static final int ARTIST_COL = TITLE_COL + 1;
    public static final int ALBUM_COL = ARTIST_COL + 1;
    public static final int LENGTH_COL = ALBUM_COL + 1;
    public static final int GENRE_COL = LENGTH_COL + 1;
    public static final int BITRATE_COL = GENRE_COL + 1;
    public static final int SIZE_COL = BITRATE_COL + 1;
    public static final int FILENAME_COL = SIZE_COL + 1;
    public static final int EXTENSION_COL = FILENAME_COL + 1;
    public static final int TRACK_COL = EXTENSION_COL + 1;
    public static final int YEAR_COL = TRACK_COL + 1;
    public static final int MODIFIED_COL = YEAR_COL + 1;
    public static final int ACTION_COL = MODIFIED_COL + 1;
    public static final int COLUMN_COUNT = ACTION_COL + 1;

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {
        switch (column) {
        case PLAY_COL:
            return "";
        case TITLE_COL:
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
        case FILENAME_COL:
            return I18n.tr("Filename");
        case EXTENSION_COL:
            return I18n.tr("Extension");
        case TRACK_COL:
            return I18n.tr("Track");
        case YEAR_COL:
            return I18n.tr("Year");
        case MODIFIED_COL:
            return I18n.tr("Modified");
        case ACTION_COL:
            return I18n.tr("Sharing");
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch (column) {
        case PLAY_COL:
            return baseObject;
        case TITLE_COL:
            if(baseObject.getProperty(FilePropertyKey.TITLE) == null)
                return baseObject.getProperty(FilePropertyKey.NAME);
            else
                return baseObject.getProperty(FilePropertyKey.TITLE);
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
        case FILENAME_COL:
            return baseObject.getProperty(FilePropertyKey.NAME);
        case EXTENSION_COL:
            return FileUtils.getFileExtension(baseObject.getFile());
        case SIZE_COL:
            return baseObject.getSize();
        case TRACK_COL:
            return baseObject.getProperty(FilePropertyKey.TRACK_NUMBER);
        case YEAR_COL:
            return baseObject.getProperty(FilePropertyKey.YEAR);
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
        return new int[] {MODIFIED_COL, YEAR_COL, TRACK_COL, EXTENSION_COL, FILENAME_COL, SIZE_COL, BITRATE_COL, GENRE_COL};
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return column == PLAY_COL || column == ACTION_COL;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }

    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case ACTION_COL:
            case PLAY_COL:
                return FileItem.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Comparator getColumnComparator(int column) {
        switch (column) {
            case PLAY_COL:
                return new NameComparator();
            case ACTION_COL:
                return new ActionComparator();
        }
        return super.getColumnComparator(column);
    }
    
    /**
     * Compares the title field in the NAME_COLUMN
     */
    private class NameComparator implements Comparator<FileItem> {
        @Override
        public int compare(FileItem o1, FileItem o2) {
            String title1 = PropertyUtils.getTitle(o1);
            String title2 = PropertyUtils.getTitle(o2);
            
            return title1.toLowerCase().compareTo(title2.toLowerCase());
        }
    }
}
