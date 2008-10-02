package org.limewire.ui.swing.menu;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeMenuBar extends JMenuBar {

    @Inject
    LimeMenuBar(HelpMenu helpMenu) {
        add(new JMenu("File"));
        add(new JMenu("Edit"));
        add(new JMenu("Tools"));
        add(helpMenu);
    }

}
