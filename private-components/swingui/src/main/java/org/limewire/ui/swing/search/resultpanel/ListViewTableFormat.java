package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a single-column table
 * that displays the list view of search results.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ListViewTableFormat
extends ResultsTableFormat<VisualSearchResult> {

    public ListViewTableFormat() {
        super(1, 0, tr("not used"));
    }

    @Override
    public Class getColumnClass(int index) {
        return index == 0 ? VisualSearchResult.class :
            super.getColumnClass(index);
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;
        return vsr;
    }

    @Override
    public int getInitialColumnWidth(int index) {
        return ListViewTableCellEditor.WIDTH;
    }
}