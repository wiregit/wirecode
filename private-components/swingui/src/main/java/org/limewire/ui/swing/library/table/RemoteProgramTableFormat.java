package org.limewire.ui.swing.library.table;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for Programs Table for LW buddies and Browse hosts
 */
public class RemoteProgramTableFormat<T extends FileItem> extends AbstractRemoteLibraryFormat<T> {
    static final int NAME_INDEX = 0;
    static final int SIZE_INDEX = 1;
    static final int EXTENSION_INDEX = 2;
    static final int PLATFORM_INDEX = 3;
    static final int COMPANY_INDEX = 4;
    static final int DESCRIPTION_INDEX = 5;
    
    public RemoteProgramTableFormat() {
        super(new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "REMOTE_LIBRARY_PROGRAM_NAME", I18n.tr("Name"), 400, true, true),     
                new ColumnStateInfo(SIZE_INDEX, "REMOTE_LIBRARY_PROGRAM_SIZE", I18n.tr("Size"), 60, true, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "REMOTE_LIBRARY_PROGRAM_EXTENSION", I18n.tr("Extension"), 60, true, true), 
                new ColumnStateInfo(PLATFORM_INDEX, "REMOTE_LIBRARY_PLATFORM_PLATFORM", I18n.tr("Platform"), 120, false, true), 
                new ColumnStateInfo(COMPANY_INDEX, "REMOTE_LIBRARY_PROGRAM_COMPANY", I18n.tr("Company"), 120, true, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "REMOTE_LIBRARY_PROGRAM_DESCRIPTION", I18n.tr("Description"), 120, false, true)
        });
    }

    @Override
    public Object getColumnValue(FileItem baseObject, int column) {
        switch(column) {
            case NAME_INDEX: return baseObject;
            case PLATFORM_INDEX: return baseObject.getProperty(FilePropertyKey.PLATFORM);
            case COMPANY_INDEX: return baseObject.getProperty(FilePropertyKey.COMPANY);
            case SIZE_INDEX: return baseObject.getSize();
            case EXTENSION_INDEX: return FileUtils.getFileExtension(baseObject.getFileName());
            case DESCRIPTION_INDEX: return baseObject.getProperty(FilePropertyKey.DESCRIPTION);
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
}