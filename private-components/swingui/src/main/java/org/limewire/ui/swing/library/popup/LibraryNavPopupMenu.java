package org.limewire.ui.swing.library.popup;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.google.inject.Inject;

public class LibraryNavPopupMenu extends JPopupMenu {

    @Inject
    public LibraryNavPopupMenu() {
        add(new JMenuItem("fake menu Item"));
        add(new JMenuItem("fake menu item 2"));
    }
}
