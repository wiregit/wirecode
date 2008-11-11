package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.JMenu;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ToolsMenu extends JMenu {

    @Inject
    public ToolsMenu(final Provider<OptionsDialog> optionDialog, final Navigator navigator) {
        super(I18n.tr("Tools"));

        add(new AbstractAction(I18n.tr("Library Manager")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me.");
            }
        });
        add(new AbstractAction(I18n.tr("Downloads")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NavItem navItem = navigator
                        .getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME);
                navItem.select();
            }
        });
        add(new AbstractAction(I18n.tr("Uploads")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me.");
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Advanced Search")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me.");
            }
        });
        add(new AbstractAction(I18n.tr("What's New Search")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me.");
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Advanced Tools")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                throw new UnsupportedOperationException("TODO implement me.");
            }
        });
        addSeparator();
        add(new AbstractAction(I18n.tr("Options")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                OptionsDialog options = optionDialog.get();
                if (!options.isVisible()) {
                    options.setLocationRelativeTo(GuiUtils.getMainFrame());
                    options.setVisible(true);
                }
            }
        });
    }
}
