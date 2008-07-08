package org.limewire.ui.swing.nav;

import javax.swing.JPanel;


public interface Navigator {

    public static enum NavItem {
        LIBRARY, LIMEWIRE
    }

    public void addNavigablePanel(Navigator.NavItem target, String name, JPanel panel, boolean userRemovable);

    public void removeNavigablePanel(Navigator.NavItem target, String name);

    public void selectNavigablePanel(Navigator.NavItem target, String name);

}