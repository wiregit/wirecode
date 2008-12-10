package org.limewire.ui.swing.library.table;

import java.util.Comparator;
import java.util.Date;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Image Table for LW buddies and Browse hosts
 */
public class RemoteImageTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    public static final int NAME_COL = 0;
    public static final int SIZE_COL = NAME_COL + 1;
    public static final int CREATED_COL = SIZE_COL + 1;
    public static final int ACTION_COL = CREATED_COL + 1;
    private static final int COLUMN_COUNT = ACTION_COL + 1;

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int column) {
        switch(column) {
            case NAME_COL:
                return I18n.tr("Filename");
            case SIZE_COL:
                return I18n.tr("Size");
            case CREATED_COL:
                return I18n.tr("Created");
            case ACTION_COL:
                return I18n.tr("Download");
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
            case NAME_COL:
                return baseObject.getName();
            case SIZE_COL:
                return baseObject.getSize();
            case CREATED_COL:
                return new Date(baseObject.getCreationTime());
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
