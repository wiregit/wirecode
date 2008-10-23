package org.limewire.ui.swing.menu;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class LimeMenuBar extends JMenuBar {

    @Inject
    LimeMenuBar(FileMenu fileMenu, HelpMenu helpMenu,
            ToolsMenu toolsMenu) {
        add(fileMenu);
        add(new JMenu(I18n.tr("Edit")));
        add(toolsMenu);
        add(helpMenu);
    }
}
