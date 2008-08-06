package org.limewire.ui.swing.sharing.table;

import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.limewire.core.api.library.FileItem;
import org.limewire.ui.swing.table.StripedJXTable;

import ca.odell.glazedlists.EventList;

public class SharingTable extends StripedJXTable {

    public SharingTable(EventList<FileItem> sharedItems) {
        super(new SharingTableModel(sharedItems));
        
        setColumnControlVisible(true);
        setShowHorizontalLines(false);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setHighlighters(HighlighterFactory.createSimpleStriping());
    }
}
