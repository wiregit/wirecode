package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Other Table for All Friends table
 */
public class AllFriendOtherTableFormat <T extends RemoteFileItem> extends RemoteOtherTableFormat<T> {
    static final int FROM_INDEX = 4;
    
    public AllFriendOtherTableFormat() {
        super(new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "ALL_LIBRARY_OTHER_NAME", I18n.tr("Name"), 450, true, true),     
                new ColumnStateInfo(TYPE_INDEX, "ALL_LIBRARY_OTHER_TYPE", I18n.tr("Type"), 60, true, true), 
                new ColumnStateInfo(EXTENSION_INDEX, "ALL_LIBRARY_OTHER_EXTENSION", I18n.tr("Extension"), 60, true, true), 
                new ColumnStateInfo(SIZE_INDEX, "ALL_LIBRARY_OTHER_SIZE", I18n.tr("Size"), 60, true, true),
                new ColumnStateInfo(FROM_INDEX, "ALL_LIBRARY_OTHER_FROM", I18n.tr("From"), 100, true, true)
        });
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
            case FROM_INDEX: return baseObject;
        }
        return super.getColumnValue(baseObject, column);
    }
}
