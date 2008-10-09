package org.limewire.ui.swing.library.table;

import java.util.Date;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FileItem.Keys;
import org.limewire.ui.swing.util.I18n;


public class DocumentTableFormat<T extends FileItem> implements LibraryTableFormat<T> {
    public static final int NAME_COL = 0;
    public static final int TYPE_COL = NAME_COL + 1;
	public static final int CREATED_COL = TYPE_COL + 1;
	public static final int SIZE_COL = CREATED_COL + 1;
	public static final int AUTHOR_COL = SIZE_COL + 1;
	public static final int MODIFIED_COL = AUTHOR_COL + 1;
	public static final int ACTION_COL = MODIFIED_COL + 1;
	private static final int COLUMN_COUNT = ACTION_COL + 1;

    @Override
    public int getColumnCount() {
        return COLUMN_COUNT;
    }

    @Override
    public String getColumnName(int column) {
    	 switch (column) {
         case AUTHOR_COL:
             return I18n.tr("Author");
         case CREATED_COL:
             return I18n.tr("Created");
         case MODIFIED_COL:
             return I18n.tr("Modified");
         case NAME_COL:
             return I18n.tr("Name");
         case ACTION_COL:
             return I18n.tr("Share");
         case SIZE_COL:
             return I18n.tr("Size");
         case TYPE_COL:
             return I18n.tr("Type");     
         }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
    	 switch (column) {
         case AUTHOR_COL:
             return baseObject.getProperty(Keys.AUTHOR);
         case CREATED_COL:
             return new Date(baseObject.getCreationTime());
         case MODIFIED_COL:
             return new Date();
         case NAME_COL:
             return baseObject;
         case ACTION_COL:
             return baseObject;
         case SIZE_COL:
             return baseObject.getSize();
         case TYPE_COL:
             return "description goes here";     
         }

         throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return ACTION_COL;
    }

    @Override
    public int[] getDefaultHiddenColums() {
        return new int[]{MODIFIED_COL, AUTHOR_COL, SIZE_COL};
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
