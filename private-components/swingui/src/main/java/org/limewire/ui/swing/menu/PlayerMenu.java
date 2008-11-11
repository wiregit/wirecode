package org.limewire.ui.swing.menu;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.limewire.ui.swing.util.I18n;

public class PlayerMenu extends JMenu {
    public PlayerMenu() {
        super(I18n.tr("Player"));

        add(new JMenuItem(I18n.tr("Play/Pause")));
        add(new JMenuItem(I18n.tr("Next")));
        add(new JMenuItem(I18n.tr("Previous")));
        addSeparator();
        add(new JMenuItem(I18n.tr("Show current file")));
    }
}
