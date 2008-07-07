package org.limewire.ui.swing.nav;

import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.mainframe.StorePanel;

public class LimeWireNavList extends NavList {
    
    public LimeWireNavList() {
        super("LimeWire");
        
        addListItem(HomePanel.NAME);
        addListItem(StorePanel.NAME);
    }

}
