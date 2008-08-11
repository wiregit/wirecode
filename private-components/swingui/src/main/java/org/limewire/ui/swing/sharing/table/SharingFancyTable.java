package org.limewire.ui.swing.sharing.table;

import javax.swing.ListSelectionModel;

import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.table.StripedJXTable;

import ca.odell.glazedlists.EventList;

public class SharingFancyTable extends StripedJXTable {

    public SharingFancyTable(EventList<FileItem> sharedItems) {
        super(new SharingTableModel(sharedItems));
        
        setColumnControlVisible(false);
        setShowGrid(false, false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFillsViewportHeight(false);
    }
}
