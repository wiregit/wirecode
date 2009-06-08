package org.limewire.ui.swing.library.popup;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class LibraryPopupMenu extends JPopupMenu {

    public LibraryPopupMenu() {
        add(new JMenuItem("Some popup item"));
        add(new JMenuItem("Some other popup"));
    }
}
