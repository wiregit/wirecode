package org.limewire.ui.swing.nav;

import javax.swing.JPanel;


public interface Navigator {

    public static enum NavCategory {
        LIBRARY, LIMEWIRE, SEARCH
    }

    public NavItem addNavigablePanel(NavCategory category, String name, JPanel panel, boolean userRemovable);

    public void removeNavigablePanel(NavCategory category, NavItem navItem);

    public void selectNavigablePanel(NavCategory category, NavItem navItem);

}