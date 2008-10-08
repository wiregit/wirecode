package org.limewire.ui.swing.library.table;

import java.util.Date;

import org.limewire.core.api.library.FileItem;


public class ImageTableFormat<T extends FileItem> implements LibraryTableFormat<T> {
    public static final int NAME_COL = 0;

    public static final int SIZE_COL = NAME_COL + 1;

    public static final int CREATED_COL = SIZE_COL + 1;

    public static final int MODIFIED_COL = CREATED_COL + 1;

    public static final int ACTION_COL = MODIFIED_COL + 1;

    private static final int COLUMN_COUNT = ACTION_COL + 1;

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        if (column == NAME_COL)
            return "Name";
        else if (column == SIZE_COL)
            return "Size";
        else if (column == CREATED_COL)
            return "Created";
        else if (column == MODIFIED_COL)
            return "Modified";
        else if (column == ACTION_COL)
            return "Share";

        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        if (column == NAME_COL)
            return baseObject.getName();
        else if (column == SIZE_COL)
            return baseObject.getSize();
        else if (column == CREATED_COL)
            return new Date();
        else if (column == MODIFIED_COL)
            return new Date(baseObject.getLastModifiedTime());
        else if (column == ACTION_COL)
            return baseObject;

        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return ACTION_COL;
    }

    @Override
    public int[] getDefaultHiddenColums() {
        return  new int[]{};
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
