package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.settings.TablesHandler;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.FileUtils;

import com.google.inject.Provider;

/**
 * Table format for Other Table for LW buddies and Browse hosts
 */
public class RemoteOtherTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    static final int NAME_INDEX = 0;
    static final int TYPE_INDEX = 1;
    static final int EXTENSION_INDEX = 2;
    static final int SIZE_INDEX = 3;
    static final int FROM_INDEX = 4;
    
    private Provider<IconManager> iconManager;
    
    public RemoteOtherTableFormat(String sortID, int sortedColumn, boolean isAscending, ColumnStateInfo[] columnInfo, 
            Provider<IconManager> iconManager) {
        super(sortID, sortedColumn, isAscending, columnInfo);
        this.iconManager = iconManager;
    }
    
    public RemoteOtherTableFormat(Provider<IconManager> iconManager) {
        super("REMOTE_LIBRARY_OTHER_TABLE", NAME_INDEX, true, new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "REMOTE_LIBRARY_OTHER_NAME", I18n.tr("Name"), 417, true, true),     
                new ColumnStateInfo(TYPE_INDEX, "REMOTE_LIBRARY_OTHER_TYPE", I18n.tr("Type"), 170, true, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "REMOTE_LIBRARY_OTHER_EXTENSION", I18n.tr("Extension"), 78, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "REMOTE_LIBRARY_OTHER_SIZE", I18n.tr("Size"), 57, true, true),
                new ColumnStateInfo(FROM_INDEX, "REMOTE_LIBRARY_OTHER_FROM", I18n.tr("From"), 32, true, true)
        });
        this.iconManager = iconManager;
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
            case NAME_INDEX: return baseObject;  
            case SIZE_INDEX: return baseObject.getSize();
            case TYPE_INDEX: 
                // Use icon manager to return MIME description.
                return (iconManager != null) ?
                    iconManager.get().getMIMEDescription(baseObject) : 
                    baseObject.getProperty(FilePropertyKey.DESCRIPTION);
            case EXTENSION_INDEX: return FileUtils.getFileExtension(baseObject.getFileName());
            case FROM_INDEX: return baseObject;
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
        switch (column) {
        case NAME_INDEX:
            return Arrays.asList(TYPE_INDEX, SIZE_INDEX);
        case TYPE_INDEX:
            return Arrays.asList(NAME_INDEX, SIZE_INDEX);
        case SIZE_INDEX:
            return Arrays.asList(NAME_INDEX, TYPE_INDEX);
        default:
            return Collections.emptyList();
        }
    }
}