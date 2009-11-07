package org.limewire.ui.swing.menu;

import java.awt.Color;

import javax.swing.JMenuBar;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class LimeMenuBar extends JMenuBar {
    
    @Resource
    private Color backgroundColor;
    
    @Inject
    LimeMenuBar(FileMenu fileMenu, FriendsMenu friendMenu, ViewMenu viewMenu, HelpMenu helpMenu,
            ToolsMenu toolsMenu, StoreMenu storeMenu) {
        
        GuiUtils.assignResources(this);
        
        setBackground(backgroundColor);
        fileMenu.setBackground(backgroundColor);
        viewMenu.setBackground(backgroundColor);
        friendMenu.setBackground(backgroundColor);
        storeMenu.setBackground(backgroundColor);
        toolsMenu.setBackground(backgroundColor);
        helpMenu.setBackground(backgroundColor);
        
        add(fileMenu);
        add(viewMenu);
        add(friendMenu);
        add(storeMenu);
        add(toolsMenu);
        add(helpMenu);
        
        MenuListener listener = new MenuListener() {
            @Override
            public void menuCanceled(MenuEvent e) {
                ((MnemonicMenu)e.getSource()).removeAll();
            }
            @Override
            public void menuDeselected(MenuEvent e) {
                ((MnemonicMenu)e.getSource()).removeAll();                
            }
            @Override
            public void menuSelected(MenuEvent e) {
                ((MnemonicMenu)e.getSource()).createMenuItems();
            }
        };
        
        fileMenu.addMenuListener(listener);
        viewMenu.addMenuListener(listener);
        friendMenu.addMenuListener(listener);
        storeMenu.addMenuListener(listener);
        toolsMenu.addMenuListener(listener);
        helpMenu.addMenuListener(listener);
    }
}
