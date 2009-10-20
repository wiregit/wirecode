package org.limewire.ui.swing.upload.table;

import java.util.Comparator;

import org.limewire.ui.swing.table.AbstractAdvancedTableFormat;

/**
 * Table format for transfer tables like the Uploads table.
 */
public abstract class TransferTableFormat<E> extends AbstractAdvancedTableFormat<E> {
    public static final int TITLE_COL = 0;
    public static final int TITLE_GAP = 1;
    public static final int PROGRESS_COL = 2;
    public static final int PROGRESS_GAP = 3;
    public static final int MESSAGE_COL = 4;
    public static final int MESSAGE_GAP = 5;
    public static final int ACTION_COL = 6;
    public static final int ACTION_GAP = 7;
    public static final int CANCEL_COL = 8;

    public TransferTableFormat() {
        super("title", "titleGap", "progress", "progress gap", "message", 
                "message gap", "action", "action gap", "cancel");
    }

    @Override
    public Comparator getColumnComparator(int column) {
        return null;
    }

    @Override
    public Object getColumnValue(E baseObject, int column) {
        return baseObject;
    }
}
