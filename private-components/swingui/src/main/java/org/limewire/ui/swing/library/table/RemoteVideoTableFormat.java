package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Video Table for LW buddies and Browse hosts
 */
public class RemoteVideoTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    public static final int NAME_COL = 0;
    public static final int EXTENSION_COL = NAME_COL + 1;
    public static final int LENGTH_COL = EXTENSION_COL + 1;
    public static final int MISC_COL = LENGTH_COL + 1;
    public static final int QUALITY_COL = MISC_COL + 1;
    public static final int SIZE_COL = QUALITY_COL + 1;
    public static final int YEAR_COL = SIZE_COL + 1;
    public static final int RATING_COL = YEAR_COL + 1;
    public static final int DIMENSION_COL = RATING_COL + 1;
    public static final int DESCRIPTION_COL = DIMENSION_COL + 1;
    public static final int COLUMN_COUNT = DESCRIPTION_COL + 1;

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {
        switch (column) {
            case NAME_COL:
                return I18n.tr("Filename");
            case EXTENSION_COL:
                return I18n.tr("Extension");
            case LENGTH_COL:
                return I18n.tr("Length");
            case MISC_COL:
                return I18n.tr("Misc");
            case QUALITY_COL:
                return I18n.tr("Quality");
            case SIZE_COL:
                return I18n.tr("Size");
            case YEAR_COL:
                return I18n.tr("Year");
            case RATING_COL:
                return I18n.tr("Rating");
            case DIMENSION_COL:
                return I18n.tr("Resolution");  
            case DESCRIPTION_COL:
                return I18n.tr("Description");
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }


    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch (column) {
            case NAME_COL:
                return baseObject.getName();
            case EXTENSION_COL:
                return FileUtils.getFileExtension(baseObject.getFileName());
            case LENGTH_COL:
                return baseObject.getProperty(FilePropertyKey.LENGTH);
            case MISC_COL:
                return baseObject.getProperty(FilePropertyKey.COMMENTS);
            case QUALITY_COL:
                return "";
            case YEAR_COL:
                return baseObject.getProperty(FilePropertyKey.YEAR);
            case RATING_COL:
                return baseObject.getProperty(FilePropertyKey.RATING);
            case SIZE_COL:
                return baseObject.getSize();
            case DESCRIPTION_COL:
                return baseObject.getProperty(FilePropertyKey.COMMENTS);
            case DIMENSION_COL:
                if(baseObject.getProperty(FilePropertyKey.WIDTH) == null || baseObject.getProperty(FilePropertyKey.HEIGHT) == null)
                    return null;
                else
                    return baseObject.getProperty(FilePropertyKey.WIDTH) + " X " + baseObject.getProperty(FilePropertyKey.HEIGHT);
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return -1;
    }

    @Override
    public int[] getDefaultHiddenColums() {
        return new int[] { DESCRIPTION_COL, DIMENSION_COL, RATING_COL, YEAR_COL};
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