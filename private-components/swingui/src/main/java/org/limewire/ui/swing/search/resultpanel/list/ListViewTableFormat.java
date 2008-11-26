package org.limewire.ui.swing.search.resultpanel.list;

import static org.limewire.ui.swing.util.I18n.tr;

import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.search.resultpanel.ResultsTableFormat;

/**
 * This class specifies the content of a single-column table
 * that displays the list view of search results.
 */
public class ListViewTableFormat extends ResultsTableFormat<VisualSearchResult> {

//    static final int ACTIONS_WIDTH = 140;

    public ListViewTableFormat() {
//        super(3, 2, tr("not used"), tr("also not used"), tr("nor this"));
        super(1, tr("not used"), tr("also not used"));
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
//        switch(index) {
//        case 0:
//            return 400;
//        case 1:
//            return 170;
//        default:
//            return ACTIONS_WIDTH;
//        }
        return index == 0 ? 400 : 170;
    }

    @Override
    public boolean isEditable(VisualSearchResult vsr, int column) {
        return column == 1;
    }
    
    @Override
    public int getNameColumn() {
        //no name column here
        return -1;
    }
}