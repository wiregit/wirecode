package org.limewire.ui.swing.library.sharing;

import java.awt.event.MouseEvent;

import org.limewire.ui.swing.table.MouseableTable;

import ca.odell.glazedlists.swing.EventTableModel;

public class ToolTipTable extends MouseableTable {
    
    private EventTableModel<SharingTarget> model;

    public ToolTipTable(EventTableModel<SharingTarget> model){
        super(model);
        this.model = model;
    }
    
    public String getToolTipText(MouseEvent event) {
        int row = rowAtPoint(event.getPoint());
        if (row >= 0){
            return model.getElementAt(row).getFriend().getRenderName();
        }
         return null;
         
     }

}
