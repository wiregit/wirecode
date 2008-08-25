package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class specifies the content of a single-column table
 * that displays the list view of search results.
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class ListViewTableFormat
extends ResultsTableFormat<VisualSearchResult> {

    public ListViewTableFormat() {
        columnNames = new String[] { "not used" };
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;
        return vsr;
    }
}