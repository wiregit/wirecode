package org.limewire.ui.swing.menu;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.limewire.ui.swing.util.I18n;

public class ViewMenu extends JMenu {
    public ViewMenu() {
        super(I18n.tr("View"));

        add(new JMenuItem(I18n.tr("Show/Hide Libraries bar")));
        add(new JMenuItem(I18n.tr("Show/Hide Download Tray")));
        add(new JMenuItem(I18n.tr("Show/Hide chat window")));
        addSeparator();
        add(new JMenuItem(I18n.tr("Search LimeWire (and Friends)")));
        add(new JMenuItem(I18n.tr("Recent Searches")));
        addSeparator();
        add(new JMenuItem(I18n.tr("List view")));
        add(new JMenuItem(I18n.tr("Classic view")));
        addSeparator();
        add(new JMenuItem(I18n.tr("Sort by")));
        add(new JMenuItem(I18n.tr("Filter current view")));
    }
}
