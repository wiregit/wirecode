package org.limewire.ui.swing.menu;

import java.awt.Color;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jdesktop.application.Resource;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.DelayedMenuItemCreator;
import org.limewire.ui.swing.mainframe.StoreMediator;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LimeMenuBar extends JMenuBar {
    
    @Resource private Color backgroundColor;
    private final MenuListener menuListener;
    
    @Inject
    LimeMenuBar(FileMenu fileMenu, FriendsMenu friendMenu, ViewMenu viewMenu, HelpMenu helpMenu,
            ToolsMenu toolsMenu, final Provider<StoreMenu> storeMenuProvider) {
        
        GuiUtils.assignResources(this);
        
        setBackground(backgroundColor);
        
        menuListener = new MenuListener() {
            @Override
            public void menuCanceled(MenuEvent e) {
                ((JMenu)e.getSource()).removeAll();
            }
            @Override
            public void menuDeselected(MenuEvent e) {
                ((JMenu)e.getSource()).removeAll();                
            }
            @Override
            public void menuSelected(MenuEvent e) {
                ((DelayedMenuItemCreator)e.getSource()).createMenuItems();
            }
        };
        
        addMenu(0, fileMenu);
        addMenu(1, viewMenu);
        addMenu(2, friendMenu);
        addMenu(3, toolsMenu);
        if (StoreMediator.canShowStoreMenu()) {
            addMenu(4, storeMenuProvider.get());
        } else {
            // if the store menu is not shown, add a listener in case the geo
            // needs to be updated.
            SwingUiSettings.SHOW_STORE_COMPONENTS.addSettingListener(new SettingListener(){
                @Override
                public void settingChanged(SettingEvent evt) {
                    if(StoreMediator.canShowStoreMenu()) {
                        SwingUtilities.invokeLater(new Runnable(){
                            public void run() {
                                addMenu(4, storeMenuProvider.get());
                            }
                        });
                    }
                }
            });
        }
        addMenu(getMenuCount(), helpMenu); // add at the end, may be idx 4 or 5, depending on store
    }
    
    private void addMenu(int idx, JMenu  menu) {
        add(menu, idx);
        menu.setBackground(backgroundColor);
        menu.addMenuListener(menuListener);
    }
    
}
