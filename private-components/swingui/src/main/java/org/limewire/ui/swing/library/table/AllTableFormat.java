package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class AllTableFormat <T extends LocalFileItem> extends AbstractLibraryFormat<T> {

    static final int NAME_INDEX = 0;
    static final int TYPE_INDEX = 1;
    static final int CREATED_INDEX = 2;
    static final int SIZE_INDEX = 3;
    static final int HIT_INDEX = 4;
    static final int UPLOADS_INDEX = 5;
    static final int UPLOAD_ATTEMPTS_INDEX = 6;
    static final int PATH_INDEX = 7;
    static final int ACTION_INDEX = 8;
    
    /** Icon manager used to find native file type information. */
    private Provider<IconManager> iconManager;
    
    @Inject
    public AllTableFormat(Provider<IconManager> iconManager) {
        super(ACTION_INDEX, "LIBRARY_ALL_TABLE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "LIBRARY_ALL_NAME", "Name", 493, true, true), 
                new ColumnStateInfo(TYPE_INDEX, "LIBRARY_ALL_TYPE", I18n.tr("Type"), 180, true, true),     
                new ColumnStateInfo(CREATED_INDEX, "LIBRARY_ALL_CREATED", I18n.tr("Date Created"), 100, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "LIBRARY_ALL_SIZE", I18n.tr("Size"), 60, true, true),
                new ColumnStateInfo(HIT_INDEX, "LIBRARY_ALL_HITS", I18n.tr("Hits"), 100, false, true), 
                new ColumnStateInfo(UPLOADS_INDEX, "LIBRARY_ALL_UPLOADS", I18n.tr("Uploads"), 100, false, true), 
                new ColumnStateInfo(UPLOAD_ATTEMPTS_INDEX, "LIBRARY_ALL_UPLOAD_ATTEMPTS", I18n.tr("Upload attempts"), 200, false, true),
                new ColumnStateInfo(PATH_INDEX, "LIBRARY_ALL_PATH", I18n.tr("Location"), 200, false, true),
                new ColumnStateInfo(ACTION_INDEX, "LIBRARY_ALL_ACTION", "", 40, true, false)
        });     
        
        this.iconManager = iconManager;
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
        case CREATED_INDEX:
            long creationTime = baseObject.getCreationTime();
            return (creationTime >= 0) ? new Date(creationTime) : null;
        case NAME_INDEX: return baseObject;
        case SIZE_INDEX: return baseObject.getSize();
        case TYPE_INDEX:
            // Use icon manager to return MIME description.
            return (iconManager != null) ?
                iconManager.get().getMIMEDescription(baseObject) : 
                baseObject.getProperty(FilePropertyKey.DESCRIPTION);
        case HIT_INDEX: return baseObject.getNumHits();
        case UPLOAD_ATTEMPTS_INDEX: return baseObject.getNumUploadAttempts();
        case UPLOADS_INDEX: return baseObject.getNumUploads();
        case PATH_INDEX: return baseObject.getProperty(FilePropertyKey.LOCATION);
        case ACTION_INDEX: return baseObject;
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }

    @Override
    public List<SortKey> getDefaultSortKeys() {
        if(TablesHandler.getSortedColumn(getSortOrderID(), getSortedColumn()).getValue() == getSortedColumn() &&
                TablesHandler.getSortedOrder(getSortOrderID(), getSortOrder()).getValue() == getSortOrder())
            return Arrays.asList(
                    new SortKey(SortOrder.ASCENDING, NAME_INDEX),
                    new SortKey(SortOrder.ASCENDING, TYPE_INDEX),
                    new SortKey(SortOrder.ASCENDING, SIZE_INDEX));
        else
            return super.getDefaultSortKeys();
    }
    
    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        return Collections.emptyList();
    }
}
