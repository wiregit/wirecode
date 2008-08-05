package org.limewire.ui.swing.search.resultpanel;

import ca.odell.glazedlists.gui.TableFormat;

import org.limewire.ui.swing.search.model.VisualSearchResult;

public class ResultTableFormat implements TableFormat<VisualSearchResult> {
    private static final String[] COLUMN_NAMES = {
        "Icon", "Name", "Type", "Size", "Actions"
    };

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES[column];
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int column) {
         if (column == 0) return vsr.getFileExtension();
         if (column == 1) return vsr.getDescription();
         if (column == 2) return vsr.getCategory().name();
         if (column == 3) return vsr.getSize();
         if (column == 4) return "action buttons go here";
         return null;
    }
}
