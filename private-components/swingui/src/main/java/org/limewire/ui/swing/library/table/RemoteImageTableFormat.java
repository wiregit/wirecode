package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Image Table for LW buddies and Browse hosts
 */
public class RemoteImageTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    public static final int NAME_COL = 0;
    public static final int EXTENSION_COL = NAME_COL + 1;
    public static final int CREATED_COL = EXTENSION_COL + 1;
    public static final int SIZE_COL = CREATED_COL + 1;
    public static final int TITLE_COL = SIZE_COL + 1;
    public static final int DESCRIPTION_COL = TITLE_COL + 1;
    private static final int COLUMN_COUNT = DESCRIPTION_COL + 1;

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
                return I18n.tr("Date Created");
            case EXTENSION_COL:
                return I18n.tr("Extension");
            case TITLE_COL:
                return I18n.tr("Title");
            case DESCRIPTION_COL:
                return I18n.tr("Description");
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
                return baseObject.getCreationTime();
            case EXTENSION_COL:
                return FileUtils.getFileExtension(baseObject.getFileName());
            case TITLE_COL:
                return baseObject.getProperty(FilePropertyKey.TITLE);
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
        return  new int[]{DESCRIPTION_COL, TITLE_COL, SIZE_COL};
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
