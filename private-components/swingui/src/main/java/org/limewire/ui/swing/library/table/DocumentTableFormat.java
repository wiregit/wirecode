package org.limewire.ui.swing.library.table;

import java.util.Date;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

/**
 * Table format for the Document Table when it is in My Library
 */
public class DocumentTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    static final int NAME_INDEX = 0;
    static final int TYPE_INDEX = 1;
    static final int CREATED_INDEX = 2;
    static final int SIZE_INDEX = 3;
    static final int AUTHOR_INDEX = 4;
    static final int DESCRIPTION_INDEX = 5;
    static final int HIT_INDEX = 6;
    static final int UPLOADS_INDEX = 7;
    static final int UPLOAD_ATTEMPTS_INDEX = 8;
    static final int ACTION_INDEX = 9;

	/** Icon manager used to find native file type information. */
	private IconManager iconManager;
	
	public DocumentTableFormat(IconManager iconManager) {
	    super(ACTION_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_DOCUMENT_NAME", "Name", 160, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "LIBRARY_DOCUMENT_TYPE", I18n.tr("Type"), 80, true, true),     
                new ColumnStateInfo(CREATED_INDEX, "LIBRARY_DOCUMENT_CREATED", I18n.tr("Date Created"), 100, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_DOCUMENT_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(AUTHOR_INDEX, "LIBRARY_DOCUMENT_AUTHOR", I18n.tr("Author"), 60, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "LIBRARY_DOCUMENT_DESCRIPTION", I18n.tr("Description"), 100, false, true), 
                new ColumnStateInfo(HIT_INDEX, "LIBRARY_DOCUMENT_HITS", I18n.tr("Hits"), 100, false, true), 
                new ColumnStateInfo(UPLOADS_INDEX, "LIBRARY_DOCUMENT_UPLOADS", I18n.tr("Uploads"), 100, false, true), 
                new ColumnStateInfo(UPLOAD_ATTEMPTS_INDEX, "LIBRARY_DOCUMENT_UPLOAD_ATTEMPTS", I18n.tr("Upload attempts"), 200, false, true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_DOCUMENT_ACTION", I18n.tr("Sharing"), 50, true, false)
        });
	    
        this.iconManager = iconManager;
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
         case AUTHOR_INDEX: return baseObject.getProperty(FilePropertyKey.AUTHOR);
         case CREATED_INDEX:
             long creationTime = baseObject.getCreationTime();
             return (creationTime >= 0) ? new Date(creationTime) : null;
         case DESCRIPTION_INDEX: return "";
         case NAME_INDEX: return baseObject;
         case ACTION_INDEX: return baseObject;
         case SIZE_INDEX: return baseObject.getSize();
         case TYPE_INDEX:
             // Use icon manager to return MIME description.
             return (iconManager != null) ?
                 iconManager.getMIMEDescription(baseObject) : 
                 baseObject.getProperty(FilePropertyKey.TOPIC);
         case HIT_INDEX: return baseObject.getNumHits();
         case UPLOAD_ATTEMPTS_INDEX: return baseObject.getNumUploadAttempts();
         case UPLOADS_INDEX: return baseObject.getNumUploads();
         }
         throw new IllegalArgumentException("Unknown column:" + column);
    }
}
