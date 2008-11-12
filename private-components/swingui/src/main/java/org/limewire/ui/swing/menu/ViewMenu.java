package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.AbstractAction;

import org.limewire.ui.swing.util.I18n;

public class ViewMenu extends JMenu {
    public ViewMenu() {
        super(I18n.tr("View"));

        add(new AbstractAction(I18n.tr("Show/Hide Libraries bar")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        add(new AbstractAction(I18n.tr("Show/Hide Download Tray")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        add(new AbstractAction(I18n.tr("Show/Hide chat window")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Search LimeWire (and Friends)")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        add(new AbstractAction(I18n.tr("Recent Searches")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("List view")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        add(new AbstractAction(I18n.tr("Classic view")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Sort by")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
        add(new AbstractAction(I18n.tr("Filter current view")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me!");
            }
        });
    }
}
