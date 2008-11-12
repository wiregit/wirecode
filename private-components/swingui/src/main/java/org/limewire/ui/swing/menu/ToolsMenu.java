package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.options.OptionsDialog;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchCategoryUtils;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class ToolsMenu extends JMenu {

    @Inject
    public ToolsMenu(final Provider<OptionsDialog> optionDialog,
            final Navigator navigator,
            SearchHandler searchHandler) {
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
        add(createWhatsNewSubmenu(searchHandler));
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
    
    private JMenu createWhatsNewSubmenu(final SearchHandler searchHandler) {
        JMenu menu = new JMenu(I18n.tr("What's New Search"));
        for(final SearchCategory category : SearchCategory.values()) {
            if (category == SearchCategory.OTHER) {
                continue;
            }
            
            Action action = new AbstractAction(SearchCategoryUtils.getName(category)) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    searchHandler.doSearch(DefaultSearchInfo.createWhatsNewSearch(category));
                }
            };            
            final JMenuItem item = menu.add(action);
            if(category == SearchCategory.PROGRAM) {
                item.setVisible(LibrarySettings.ALLOW_PROGRAMS.getValue());
                LibrarySettings.ALLOW_PROGRAMS.addSettingListener(new SettingListener() {
                    @Override
                    public void settingChanged(SettingEvent evt) {
                        item.setVisible(LibrarySettings.ALLOW_PROGRAMS.getValue());
                    }
                });
            }
        }
        return menu;
    }
}
