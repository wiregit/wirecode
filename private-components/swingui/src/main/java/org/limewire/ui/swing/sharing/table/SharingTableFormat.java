package org.limewire.ui.swing.sharing.table;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.gui.TableFormat;

/**
 * Headers and column names for non fancy sharing table.
 */
public class SharingTableFormat implements TableFormat<LocalFileItem> {

    public static final String[] columnLabels = new String[] {I18n.tr("Name"), I18n.tr("Size"), I18n.tr("Created"), I18n.tr("Modified"), I18n.tr("Hits"), I18n.tr("Uploads"), I18n.tr("Actions")};
        
    @Override
    public int getColumnCount() {
        return columnLabels.length;
    }

    @Override
    public String getColumnName(int column) {
        if(column < 0 || column >= columnLabels.length)
            throw new IllegalStateException("Unknown column:" + column);

        return columnLabels[column];
    }

    @Override
    public Object getColumnValue(LocalFileItem baseObject, int column) {
        if(column == 0) return baseObject;
        else if(column == 1) return baseObject.getSize();
        else if(column == 2) return baseObject.getCreationTime();
        else if(column == 3) return baseObject.getLastModifiedTime();
        else if(column == 4) return baseObject.getNumHits();
        else if(column == 5) return baseObject.getNumUploads();
        else if(column == 6) return baseObject;
        
        throw new IllegalStateException("Unknown column:" + column);
    }        
}
