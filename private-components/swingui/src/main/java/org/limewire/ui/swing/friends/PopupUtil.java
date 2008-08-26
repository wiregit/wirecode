package org.limewire.ui.swing.friends;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;

class PopupUtil {

    public static JPopupMenu addPopupMenus(JTextComponent component, Action... actions) {
        JPopupMenu menu = new JPopupMenu();
        for(Action action : actions) {
            menu.add(action);
        }
        PopupListener popupListener = new PopupListener(menu);
        component.addMouseListener(popupListener);
        menu.addPopupMenuListener(popupListener);
        return menu;
    }

    /**
     * Handles displaying the popup.  Also sets the enablement of each menuitem in a JPopupMenu
     * based on its action's enablement. 
     */
    private static class PopupListener extends MouseAdapter implements PopupMenuListener {
        private final  JPopupMenu popup;
        
        public PopupListener(JPopupMenu popup) {
            this.popup = popup;
        }
        
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(),
                           e.getX(), e.getY());
            }
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {
            //no-op
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            //no-op
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            for(Component comp : popup.getComponents()) {
                if (comp instanceof JMenuItem) {
                    JMenuItem item = (JMenuItem)comp;
                    if (item.getAction() != null) {
                        item.setEnabled(item.getAction().isEnabled());
                    }
                }
            }
        }
    }
}
