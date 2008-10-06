package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.util.I18n;

public class OtherTableFormat<T extends FileItem> implements LibraryTableFormat<T> {

    public static final int NAME_COL = 0;
    public static final int TYPE_COL = NAME_COL + 1;
    public static final int SIZE_COL = TYPE_COL + 1;
    public static final int FILE_COUNT_COL = SIZE_COL + 1;
    public static final int MODIFIED_COL = FILE_COUNT_COL + 1;
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
        case SIZE_COL:
            return I18n.tr("Size");
        case FILE_COUNT_COL:
            return I18n.tr("Number of Files");
        case TYPE_COL:
            return I18n.tr("Type");
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
        case NAME_COL:
            return baseObject.getName();  
        case SIZE_COL:
            return baseObject.getSize();
        case FILE_COUNT_COL:
            return 1999;
        case TYPE_COL:
            return "Verbal description";
        case MODIFIED_COL:
            return baseObject.getLastModifiedTime();
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
        return new int[] { MODIFIED_COL};
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return column == ACTION_COL;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }

 
}
