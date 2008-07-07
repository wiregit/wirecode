package org.limewire.ui.swing.nav;

import org.limewire.ui.swing.home.HomePanel;
import org.limewire.ui.swing.mainframe.StorePanel;
import org.limewire.ui.swing.nav.Navigator.NavItem;

public class LimeWireNavList extends NavList {
    
    public LimeWireNavList(Navigator navigator) {
        super("LimeWire", NavItem.LIMEWIRE, navigator);
        
        addNavItem(new HomePanel(), HomePanel.NAME);
        addNavItem(new StorePanel(), StorePanel.NAME);
    }

    public void goHome() {
        navigateToItem(HomePanel.NAME);
    }

}
