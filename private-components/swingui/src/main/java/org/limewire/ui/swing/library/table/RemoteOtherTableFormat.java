package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for Other Table for LW buddies and Browse hosts
 */
public class RemoteOtherTableFormat<T extends FileItem> extends AbstractRemoteLibraryFormat<T> {
    public static final int NAME_COL = 0;
    public static final int TYPE_COL = NAME_COL + 1;
    public static final int EXTENSION_COL = TYPE_COL + 1;
    public static final int SIZE_COL = EXTENSION_COL + 1;
    public static final int COLUMN_COUNT = SIZE_COL + 1;

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
            case EXTENSION_COL:
                return I18n.tr("Extension");
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
            case EXTENSION_COL:
                return FileUtils.getFileExtension(baseObject.getFileName());
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return -1;
    }

    @Override
    public int[] getDefaultHiddenColums() {
        return new int[] { };
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        return false;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }
}