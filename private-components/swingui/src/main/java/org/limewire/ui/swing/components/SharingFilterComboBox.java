package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Resource;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.library.SharingMatchingEditor;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Drop down combo box for filtering My Library with a Sharing View.
 * The view remains My Library but only shows files that are currently shared
 * with the selected friend.
 */
public class SharingFilterComboBox extends LimeComboBox {

    @Resource
    private Icon gnutellaIcon;
    @Resource
    private Icon friendIcon;
    @Resource
    private Color labelColor;
    @Resource
    private Font labelFont;
    @Resource
    private Color friendColor;
    @Resource
    private Font friendFont;
    
    private final Set<Friend> menuList = new HashSet<Friend>();

    private final SharingMatchingEditor matchingEditor;
    private final MyLibraryPanel myLibraryPanel;
    private final ShareListManager shareListManager;
    
    private JPopupMenu menu = new JPopupMenu();
    
    private JComponent subMenuText;
        
    public SharingFilterComboBox(SharingMatchingEditor matchingEditor, MyLibraryPanel myLibraryPanel, ShareListManager shareListManager) {
        this.matchingEditor = matchingEditor;
        this.myLibraryPanel = myLibraryPanel;
        this.shareListManager = shareListManager;
        
        GuiUtils.assignResources(this);
        
        overrideMenu(menu);
        
        subMenuText = decorateLabel(new JLabel(I18n.tr("with:")));
        
        SharingListener listener = new SharingListener();
        menu.addPopupMenuListener(listener);
    }
    
    /**
	 * Selects a friend in the combo box. This is a convience method for
	 * selecting a friend share view programatically outside of My Library.
	 */
    public void selectFriend(Friend friend) {
        matchingEditor.setFriend(friend);

        MenuAction action = new MenuAction(friend, 0, null);
        fireChangeEvent(action);
    }
    
    /**
	 * Adds a friend to the list to be displayed.
	 */
    public void addFriend(Friend friend) {
        menuList.add(friend);
    }
    
    /**
	 * Removes a friend from the list to be displayed.
	 */
    public void removeFriend(Friend friend) {
        menuList.remove(friend);
        if(menuList.size() == 0)
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    myLibraryPanel.showAllFiles();  
                }
            });
    }
    
    private JComponent decorateLabel(JComponent component) {
        component.setForeground(labelColor);
        component.setFont(labelFont);
        component.setBorder(BorderFactory.createEmptyBorder(0,2,0,0));
        return component;
    }
    
    private JMenuItem decorateItem(AbstractAction action) {
        JMenuItem item = new JMenuItem(action);
        item.setForeground(friendColor);
        item.setFont(friendFont);
        return item;
    }
         
    /**
     * MenuAction for a shared list
     */
    private class MenuAction extends AbstractAction {
        private final Friend friend;
        
        public MenuAction(Friend friend, int count, Icon icon) {
            this.friend = friend;
            if(friend != null)
                putValue(Action.NAME, friend.getRenderName() + "  (" + count + ")");
            putValue(Action.SMALL_ICON, icon);
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
    
    /**
     * Listens for combo box selection and creates the 
     * menu of shared lists to choose from.
     */
    private class SharingListener implements PopupMenuListener {        
        @Override
        public void popupMenuCanceled(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
        
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            menu.removeAll();        
            menu.add(subMenuText);
           
            menu.add(decorateItem(new MenuAction(SharingTarget.GNUTELLA_SHARE.getFriend(), shareListManager.getGnutellaShareList().size(), gnutellaIcon)));
            
            List<Friend> sortedFriends = new ArrayList<Friend>(menuList);
            Collections.sort(sortedFriends, new FriendComparator());
            for(Friend friend : sortedFriends) {
                menu.add(decorateItem(new MenuAction(friend, shareListManager.getFriendShareList(friend).size(), friendIcon)));
            }
        }
    }
    
    /**
     * Compares two friends names for sorting.
     */
    private static class FriendComparator implements Comparator<Friend> {
        @Override
        public int compare(Friend o1, Friend o2) {
            if(o1 == o2) {
                return 0;
            } else {           
                return o1.getRenderName().compareToIgnoreCase(o2.getRenderName());
            }
        }
    }
}
