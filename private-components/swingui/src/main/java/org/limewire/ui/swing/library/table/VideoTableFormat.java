package org.limewire.ui.swing.library.table;

import java.util.Comparator;
import java.util.Date;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Video Table when it is in My Library
 */
public class VideoTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    public static final int NAME_COL = 0;
    public static final int LENGTH_COL = NAME_COL + 1;
    public static final int MISC_COL = LENGTH_COL + 1;
    public static final int YEAR_COL = MISC_COL + 1;
    public static final int SIZE_COL = YEAR_COL + 1;
    public static final int RATING_COL = SIZE_COL + 1;
    public static final int COMMENTS_COL = RATING_COL + 1;
    public static final int MODIFIED_COL = COMMENTS_COL + 1;
    public static final int HEIGHT_COL = MODIFIED_COL + 1;
    public static final int WIDTH_COL = HEIGHT_COL + 1;
    public static final int ACTION_COL = WIDTH_COL + 1;
    public static final int COLUMN_COUNT = ACTION_COL + 1;

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {
        switch (column) {
        case NAME_COL:
            return I18n.tr("Name");
        case LENGTH_COL:
            return I18n.tr("Length");
        case MISC_COL:
            return I18n.tr("Miscellaneous");
        case YEAR_COL:
            return I18n.tr("Year");
        case RATING_COL:
            return I18n.tr("Rating");
        case SIZE_COL:
            return I18n.tr("Size");
        case COMMENTS_COL:
            return I18n.tr("Comments");
        case HEIGHT_COL:
            return I18n.tr("Height");
        case MODIFIED_COL:
            return I18n.tr("Modified");
        case ACTION_COL:
            return I18n.tr("Sharing");    
        case WIDTH_COL:
            return I18n.tr("Width");  
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }


    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch (column) {
        case NAME_COL:
            return baseObject.getName();
        case LENGTH_COL:
            return baseObject.getProperty(FilePropertyKey.LENGTH);
        case MISC_COL:
            return baseObject.getProperty(FilePropertyKey.COMMENTS);
        case YEAR_COL:
            return baseObject.getProperty(FilePropertyKey.YEAR);
        case RATING_COL:
            return baseObject.getProperty(FilePropertyKey.RATING);
        case SIZE_COL:
            return baseObject.getSize();
        case COMMENTS_COL:
            return baseObject.getProperty(FilePropertyKey.COMMENTS);
        case HEIGHT_COL:
            return baseObject.getProperty(FilePropertyKey.HEIGHT); 
        case WIDTH_COL:
            return baseObject.getProperty(FilePropertyKey.WIDTH); 
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
        return new int[] { WIDTH_COL, HEIGHT_COL, MODIFIED_COL, COMMENTS_COL, RATING_COL, SIZE_COL};
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return column == ACTION_COL;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }
    
    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case ACTION_COL:
                return FileItem.class;
        }
        return super.getColumnClass(column);
    }

    @Override
    public Comparator getColumnComparator(int column) {
        switch (column) {
            case ACTION_COL:
                return new ActionComparator();
        }
        return super.getColumnComparator(column);
    }
}
