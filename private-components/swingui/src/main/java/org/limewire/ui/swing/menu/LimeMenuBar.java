package org.limewire.ui.swing.menu;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeMenuBar extends JMenuBar {

    @Inject
    LimeMenuBar(HelpMenu helpMenu, ToolsMenu toolsMenu) {
        add(new JMenu("File"));
        add(new JMenu("Edit"));
        add(toolsMenu);
        add(helpMenu);
    }

}
