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

    static final int ACTIONS_WIDTH = 140;

    public ListViewTableFormat() {
        super(3, 2, tr("not used"), tr("also not used"), tr("nor this"));
    }

    @Override
    public Class getColumnClass(int index) {
        return VisualSearchResult.class;
    }

    @Override
    public Object getColumnValue(VisualSearchResult vsr, int index) {
        this.vsr = vsr;
        return vsr;
    }

    @Override
    public int getInitialColumnWidth(int index) {
        switch(index) {
        case 0:
            return 400;
        case 1:
            return 170;
        default:
            return ACTIONS_WIDTH;
        }
    }
}