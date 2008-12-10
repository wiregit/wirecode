package org.limewire.ui.swing.library.table;

import java.util.Comparator;
import java.util.Date;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Other Table when it is in My Library
 */
public class OtherTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    public static final int NAME_COL = 0;
    public static final int TYPE_COL = NAME_COL + 1;
    public static final int SIZE_COL = TYPE_COL + 1;
    public static final int MODIFIED_COL = SIZE_COL + 1;
    public static final int EXTENSION_COL = MODIFIED_COL + 1;
    public static final int ACTION_COL = EXTENSION_COL + 1;
    public static final int COLUMN_COUNT = ACTION_COL + 1;

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {
        switch (column) {
        case NAME_COL:
            return I18n.tr("Filename");        
        case SIZE_COL:
            return I18n.tr("Size");
        case TYPE_COL:
            return I18n.tr("Type");
        case MODIFIED_COL:
            return I18n.tr("Modified");
        case EXTENSION_COL:
            return I18n.tr("Extension");
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
        case SIZE_COL:
            return baseObject.getSize();
        case TYPE_COL:
            return "Verbal description";
        case MODIFIED_COL:
            return new Date(baseObject.getLastModifiedTime());
        case EXTENSION_COL:
            return FileUtils.getFileExtension(baseObject.getFile());
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
        return new int[] { EXTENSION_COL, MODIFIED_COL};
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
