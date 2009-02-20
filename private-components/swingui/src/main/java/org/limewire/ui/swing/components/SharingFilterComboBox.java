package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.SharingMatchingEditor;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.util.I18n;

/**
 * Drop down combo box for filtering My Library with a Sharing View.
 */
public class SharingFilterComboBox extends LimeComboBox {

    private final Set<Friend> menuList = new HashSet<Friend>();

    private final SharingMatchingEditor matchingEditor;

    private JPopupMenu menu = new JPopupMenu();
    
    public SharingFilterComboBox(SharingMatchingEditor matchingEditor) {
        this.matchingEditor = matchingEditor;
        
        overrideMenu(menu);
        
        setText(I18n.tr("What I'm Sharing"));
        
        SharingListener listener = new SharingListener();
        menu.addPopupMenuListener(listener);
    }
    
    public void selectFriend(Friend friend) {
        matchingEditor.setFriend(friend);

        MenuAction action = new MenuAction(friend);
        fireChangeEvent(action);
    }
    
    public void addFriend(Friend friend) {
        menuList.add(friend);
    }
    
    public void removeFriend(Friend friend) {
        menuList.remove(friend);
    }
        
    private class MenuAction extends AbstractAction {
        private final Friend friend;
        
        public MenuAction(Friend friend) {
            this.friend = friend;
            if(friend != null)
                putValue(Action.NAME, friend.getRenderName());
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            matchingEditor.setFriend(friend);
            
            SharingFilterComboBox.this.fireChangeEvent(this);
        }
        
        public String toString() {
            return friend.getRenderName();
        }
    }
    
    private class SharingListener implements PopupMenuListener {        
        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            menu.removeAll();         
           
            menu.add(new MenuAction(SharingTarget.GNUTELLA_SHARE.getFriend()));
            for(Friend friend : menuList) {
                menu.add(new MenuAction(friend));
            }
        }
    }
}
