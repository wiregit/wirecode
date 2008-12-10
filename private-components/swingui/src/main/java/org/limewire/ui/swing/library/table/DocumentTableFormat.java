package org.limewire.ui.swing.library.table;

import java.util.Comparator;
import java.util.Date;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.FileUtils;

/**
 * Table format for the Document Table when it is in My Library
 */
public class DocumentTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    public static final int NAME_COL = 0;
    public static final int TYPE_COL = NAME_COL + 1;
	public static final int CREATED_COL = TYPE_COL + 1;
	public static final int SIZE_COL = CREATED_COL + 1;
	public static final int AUTHOR_COL = SIZE_COL + 1;
	public static final int MODIFIED_COL = AUTHOR_COL + 1;
	public static final int EXTENSION_COL = MODIFIED_COL + 1;
	public static final int ACTION_COL = EXTENSION_COL + 1;
	private static final int COLUMN_COUNT = ACTION_COL + 1;

	/** Icon manager used to find native file type information. */
	private IconManager iconManager;
    
	/**
	 * Constructs a DocumentTableFormat.
	 */
    public DocumentTableFormat() {
    }
	
    /**
     * Constructs a DocumentTableFormat with the specified icon manager.
     */
	public DocumentTableFormat(IconManager iconManager) {
	    this.iconManager = iconManager;
	}
	
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
         case EXTENSION_COL:
             return I18n.tr("Extension");
         case ACTION_COL:
             return I18n.tr("Sharing");
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
             return baseObject.getProperty(FilePropertyKey.AUTHOR);
         case CREATED_COL:
             // Return creation time if valid.
             long creationTime = baseObject.getCreationTime();
             return (creationTime >= 0) ? new Date(creationTime) : null;
         case MODIFIED_COL:
             // Return last modified time. 
             return new Date(((LocalFileItem) baseObject).getLastModifiedTime());
         case NAME_COL:
             return baseObject;
         case EXTENSION_COL:
             return FileUtils.getFileExtension(baseObject.getFile());
         case ACTION_COL:
             return baseObject;
         case SIZE_COL:
             return baseObject.getSize();
         case TYPE_COL:
             // Use icon manager to return MIME description.
             return (iconManager != null) ?
                 iconManager.getMIMEDescription(baseObject) : 
                 baseObject.getProperty(FilePropertyKey.TOPIC);
         }
         throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public int getActionColumn() {
        return ACTION_COL;
    }

    @Override
    public int[] getDefaultHiddenColums() {
        return new int[] {EXTENSION_COL, AUTHOR_COL, SIZE_COL, CREATED_COL};
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
