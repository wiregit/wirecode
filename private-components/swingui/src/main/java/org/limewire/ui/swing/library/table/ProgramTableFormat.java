package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Program Table when it is in My Library
 */
public class ProgramTableFormat<T extends LocalFileItem> extends AbstractMyLibraryFormat<T> {
    static final int NAME_INDEX = 0;
    static final int SIZE_INDEX = 1;
    static final int PLATFORM_INDEX = 2;
    static final int COMPANY_INDEX = 3;
    static final int DESCRIPTION_INDEX = 4;
    static final int HIT_INDEX = 5;
    static final int UPLOADS_INDEX = 6;
    static final int UPLOAD_ATTEMPTS_INDEX = 7;
    static final int ACTION_INDEX = 8;
    
    public ProgramTableFormat() {
        super(ACTION_INDEX, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_PROGRAM_NAME", I18n.tr("Name"), 250, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_PROGRAM_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(PLATFORM_INDEX, "LIBRARY_PROGRAM_PLATFORM", I18n.tr("Platform"), 120, false, true), 
                new ColumnStateInfo(COMPANY_INDEX, "LIBRARY_PROGRAM_COMPANY", I18n.tr("Company"), 120, true, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "LIBRARY_PROGRAM_DESCRIPTION", I18n.tr("Description"), 120, false, true), 
                new ColumnStateInfo(HIT_INDEX, "LIBRARY_PROGRAM_HITS", I18n.tr("Hits"), 100, false, true), 
                new ColumnStateInfo(UPLOADS_INDEX, "LIBRARY_PROGRAM_UPLOADS", I18n.tr("Uploads"), 100, false, true), 
                new ColumnStateInfo(UPLOAD_ATTEMPTS_INDEX, "LIBRARY_PROGRAM_UPLOAD_ATTEMPTS", I18n.tr("Upload attempts"), 200, false, true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_PROGRAM_ACTION", I18n.tr("Sharing"), 50, true, false)
        });
    }

    @Override
    public Object getColumnValue(LocalFileItem baseObject, int column) {
        switch(column) {
        case NAME_INDEX: return baseObject;
        case PLATFORM_INDEX: return baseObject.getProperty(FilePropertyKey.PLATFORM);
        case COMPANY_INDEX: return baseObject.getProperty(FilePropertyKey.COMPANY);
        case SIZE_INDEX: return baseObject.getSize();
        case DESCRIPTION_INDEX: return "";
        case HIT_INDEX: return baseObject.getNumHits();
        case UPLOAD_ATTEMPTS_INDEX: return baseObject.getNumUploadAttempts();
        case UPLOADS_INDEX: return baseObject.getNumUploads();
        case ACTION_INDEX: return baseObject;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
}
