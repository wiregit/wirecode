package org.limewire.ui.swing.library.table;

import java.util.Date;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileItem.Keys;
import org.limewire.ui.swing.util.I18n;


public class VideoTableFormat<T extends FileItem> implements LibraryTableFormat<T> {
  //  public static final int PLAY_COL = 0;
    public static final int NAME_COL = 0;//PLAY_COL + 1;
    public static final int LENGTH_COL = NAME_COL + 1;
    public static final int MISC_COL = LENGTH_COL + 1;
    public static final int YEAR_COL = MISC_COL + 1;
    public static final int SIZE_COL = YEAR_COL + 1;
    public static final int RATING_COL = SIZE_COL + 1;
    public static final int COMMENTS_COL = RATING_COL + 1;
    public static final int MODIFIED_COL = COMMENTS_COL + 1;
    public static final int HEIGHT_COL = MODIFIED_COL + 1;
    public static final int WIDTH_COL = HEIGHT_COL + 1;
    public static final int BITRATE_COL = WIDTH_COL + 1;
    public static final int ACTION_COL = BITRATE_COL + 1;
    public static final int COLUMN_COUNT = ACTION_COL + 1;


    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    public String getColumnName(int column) {

        switch (column) {
//        case PLAY_COL:
//            return I18n.tr("Play");
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
        case BITRATE_COL:
            return I18n.tr("Bitrate");
        case SIZE_COL:
            return I18n.tr("Size");
        case COMMENTS_COL:
            return I18n.tr("Comments");
        case HEIGHT_COL:
            return I18n.tr("Height");
        case MODIFIED_COL:
            return I18n.tr("Modified");
        case ACTION_COL:
            return I18n.tr("Share");    
        case WIDTH_COL:
            return I18n.tr("Width");  
        }

        throw new IllegalArgumentException("Unknown column:" + column);
    }


    @Override
    public Object getColumnValue(FileItem baseObject, int column) {

        switch (column) {
//        case PLAY_COL:
//            return baseObject;
        case NAME_COL:
            return baseObject.getName();
        case LENGTH_COL:
            return baseObject.getProperty(Keys.LENGTH);
        case MISC_COL:
            return baseObject.getProperty(Keys.MISCELLANEOUS);
        case YEAR_COL:
            return baseObject.getProperty(Keys.YEAR);
        case RATING_COL:
            return baseObject.getProperty(Keys.RATING);
        case BITRATE_COL:
            return baseObject.getProperty(Keys.BITRATE);
        case SIZE_COL:
            return baseObject.getSize();
        case COMMENTS_COL:
            return baseObject.getProperty(Keys.COMMENTS);
        case HEIGHT_COL:
            return baseObject.getProperty(Keys.HEIGHT);
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
        return new int[] { BITRATE_COL, WIDTH_COL, HEIGHT_COL, MODIFIED_COL, COMMENTS_COL, RATING_COL, SIZE_COL};
    }

    @Override
    public boolean isEditable(T baseObject, int column) {
        //return column == PLAY_COL || column == ACTION_COL;
        return column == ACTION_COL;
    }

    @Override
    public T setColumnValue(T baseObject, Object editedValue, int column) {
        return baseObject;
    }


}
