package org.limewire.ui.swing.menu;

import javax.swing.JMenuBar;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeMenuBar extends JMenuBar {

    @Inject
    LimeMenuBar(FileMenu fileMenu, ViewMenu viewMenu, PlayerMenu playerMenu, HelpMenu helpMenu,
            ToolsMenu toolsMenu) {
        add(fileMenu);
        add(viewMenu);
        add(playerMenu);
        add(toolsMenu);
        add(helpMenu);
    }
}
