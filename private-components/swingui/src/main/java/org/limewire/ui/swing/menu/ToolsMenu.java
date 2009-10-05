package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.event.MenuListener;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.inspection.DataCategory;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.advanced.AdvancedToolsPanel;
import org.limewire.ui.swing.mainframe.OptionsAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchCategoryUtils;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.search.SearchNavItem;
import org.limewire.ui.swing.search.SearchNavigator;
import org.limewire.ui.swing.upload.UploadMediator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * The Tools menu in the main menubar.
 */
class ToolsMenu extends MnemonicMenu {

    private final JMenu whatsNewSubmenu;
    
    private final Provider<AdvancedToolsPanel> advancedToolsPanelProvider; 
    private final Provider<Navigator> navigatorProvider;
    private final Provider<UploadMediator> uploadMediatorProvider;
    private final Provider<SearchHandler> searchHandlerProvider;
    private final Provider<SearchNavigator> searchNavigatorProvider;
    private final Provider<OptionsAction> optionsAction;
    
    @InspectablePrimitive(value = "search view", category = DataCategory.USAGE)
    private static volatile int uploadsViewed = 0;
    
    private AdvancedToolsPanel advancedTools = null;
    
    @Inject
    public ToolsMenu(
            Provider<AdvancedToolsPanel> advancedToolsPanelProvider, 
            Provider<Navigator> navigatorProvider, 
            Provider<UploadMediator> uploadMediatorProvider,
            Provider<SearchHandler> searchHandlerProvider, 
            Provider<SearchNavigator> searchNavigatorProvider,
            Provider<OptionsAction> optionsAction) {
        
        super(I18n.tr("&Tools"));
        
        this.advancedToolsPanelProvider = advancedToolsPanelProvider;
        this.navigatorProvider = navigatorProvider;
        this.uploadMediatorProvider = uploadMediatorProvider;
        this.searchHandlerProvider = searchHandlerProvider;
        this.searchNavigatorProvider = searchNavigatorProvider;
        this.optionsAction = optionsAction;

        whatsNewSubmenu = createWhatsNewSubmenu();
    }
    
    @Override
    public void createMenuItems() {
        add(new AbstractAction(I18n.tr("&Uploads")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                
                NavItem navItem = navigatorProvider.get().getNavItem(NavCategory.UPLOAD, UploadMediator.NAME);
                if (navItem == null) {
                    navItem = navigatorProvider.get().createNavItem(NavCategory.UPLOAD, UploadMediator.NAME, uploadMediatorProvider.get());
                }
                navItem.select();
                uploadsViewed++;
            }
        });
        
        addSeparator();
        add(new AbstractAction(I18n.tr("Advanced &Search")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SearchNavItem item = searchNavigatorProvider.get().addAdvancedSearch();
                item.select();
            }
        });
        
        add(whatsNewSubmenu);
        addSeparator();
        
        add(new AbstractAction(I18n.tr("&Advanced Tools...")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                // If the existing Advanced Tools panel is null, then create a
                // new one, along with a listener to clear the reference when
                // the window is closed.
                WindowListener closeListener = null;
                if (advancedTools == null) {
                    advancedTools = advancedToolsPanelProvider.get();
                    closeListener = new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            advancedTools = null;
                        }
                    };
                }
                advancedTools.display(closeListener);
            }
        });
        if (!OSUtils.isMacOSX()) {
            addSeparator();
            add(optionsAction.get());
        }   
    }
    
    @Override
    public void addMenuListener(MenuListener listener) {
        super.addMenuListener(listener);
        whatsNewSubmenu.addMenuListener(listener);
    }

    private JMenu createWhatsNewSubmenu() {
        JMenu menu = new MnemonicMenu(I18n.tr("&What's New Search")) {
            @Override
            public void createMenuItems() {
                for (final SearchCategory category : SearchCategory.values()) {
                    if (category == SearchCategory.OTHER) {
                        continue;
                    }

                    Action action = new AbstractAction(SearchCategoryUtils.getWhatsNewMenuName(category)) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            searchHandlerProvider.get().doSearch(DefaultSearchInfo.createWhatsNewSearch(category));
                        }
                    };
                    add(action);
                }
            }
        };
            
        return menu;
    }
}
