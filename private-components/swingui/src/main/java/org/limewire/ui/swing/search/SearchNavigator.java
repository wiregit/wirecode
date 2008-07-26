package org.limewire.ui.swing.search;

import javax.swing.JComponent;

import org.limewire.ui.swing.nav.NavItem;

public interface SearchNavigator {
    
    NavItem addSearch(String title, JComponent search);

}
