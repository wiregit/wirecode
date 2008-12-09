package org.limewire.ui.swing.library.table;

import java.util.Comparator;
import java.util.Date;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PropertyUtils;

/**
 * Table format for the Audio Table when it is in My Library
 */
public class AudioTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    public static final int NAME_COL = 0;
    public static final int ARTIST_COL = NAME_COL + 1;
    public static final int ALBUM_COL = ARTIST_COL + 1;
    public static final int LENGTH_COL = ALBUM_COL + 1;
    public static final int GENRE_COL = LENGTH_COL + 1;
    public static final int BITRATE_COL = GENRE_COL + 1;
    public static final int SIZE_COL = BITRATE_COL + 1;
    public static final int FILE_COL = SIZE_COL + 1;
    public static final int TRACK_COL = FILE_COL + 1;
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
        case NAME_COL:
            return baseObject;
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
        return new int[] {MODIFIED_COL, YEAR_COL, TRACK_COL, FILE_COL, SIZE_COL, BITRATE_COL};
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return column == NAME_COL || column == ACTION_COL;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }

    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case ACTION_COL:
            case NAME_COL:
                return FileItem.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Comparator getColumnComparator(int column) {
        switch (column) {
            case NAME_COL:
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
