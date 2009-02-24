package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
import org.limewire.ui.swing.library.MyLibraryPanel;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.library.sharing.model.MultiFileShareModel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.XMPPService;

/**
 * Share All combo Box. In My Library view allows the user
 * to share/unshare all the files with Gnutella or a Friend
 */
public class ShareAllComboBox extends LimeComboBox {

    @Resource
    private Color labelColor;
    @Resource
    private Font labelFont;
    @Resource
    private Color disabledColor;
    
    private final XMPPService xmppService;
    private final FriendsSignInPanel friendsSignInPanel;
    private final ShareWidgetFactory shareWidgetFactory;
    private final MyLibraryPanel myLibraryPanel;
    private final ShareListManager shareListManager;
    
    private JPopupMenu menu = new JPopupMenu();
    
    private AbstractAction shareAllAction;
    private AbstractAction unshareAllAction;
    private AbstractAction shareAllFriendAction;
    private AbstractAction unshareAllFriendAction;
    private AbstractAction signedOutAction;
    
    public ShareAllComboBox(ShareListManager shareListManager, XMPPService xmppService, FriendsSignInPanel friendsSignInPanel, ShareWidgetFactory shareWidgetFactory,
            MyLibraryPanel myLibraryPanel) {
        this.xmppService = xmppService;
        this.friendsSignInPanel = friendsSignInPanel;
        this.shareWidgetFactory = shareWidgetFactory;
        this.myLibraryPanel = myLibraryPanel;
        this.shareListManager = shareListManager;
                
        GuiUtils.assignResources(this);
        
        overrideMenu(menu);
        
        createActions();
        
        SharingListener listener = new SharingListener();
        menu.addPopupMenuListener(listener);
    }
    
    private void createActions() {
        shareAllAction = new ShareAllAction(true);
        unshareAllAction = new UnShareAllAction(true);
        shareAllFriendAction = new ShareAllAction(false);
        unshareAllFriendAction = new UnShareAllAction(false);
        signedOutAction = new SignedOutAction();
    }
    
    private JMenuItem decorateItem(AbstractAction action) {
        JMenuItem item = new JMenuItem(action);
        item.setForeground(labelColor);
        item.setFont(labelFont);
        return item;
    }
    
    private JMenuItem decorateDisabledfItem(AbstractAction action) {
        JMenuItem item = new JMenuItem(action);
        item.setForeground(disabledColor);
        item.setFont(labelFont);
        return item;
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
            
            // if not logged in don't show options for friends.
            if(!xmppService.isLoggedIn()) {
                menu.add(decorateItem(shareAllAction));
                menu.add(decorateItem(unshareAllAction));
                
                menu.addSeparator();
                
                menu.add(decorateDisabledfItem(signedOutAction));
            } else {
                menu.add(decorateItem(shareAllAction));
                menu.add(decorateItem(shareAllFriendAction));
                
                menu.addSeparator();
                
                menu.add(decorateItem(unshareAllAction));
                menu.add(decorateItem(unshareAllFriendAction));
            }
        }
    }
    
    /**
	 *	If Gnutella is chosen, shares all the files in the current table with Gnutella.
	 *  If a friend is selected, opens the Multi-file share widget
	 */
    private class ShareAllAction extends AbstractAction {    
        private final boolean isGnutella;
        
        public ShareAllAction(boolean isGnutella) {
            this.isGnutella = isGnutella;
            
            if(!isGnutella) {
                putValue(Action.NAME, I18n.tr("Share all with friend..."));
            } else {
                putValue(Action.NAME, I18n.tr("Share all with P2P Network"));
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            SelectAllable<LocalFileItem> selectAllable = myLibraryPanel.getTable();
            selectAllable.selectAll();
            List<LocalFileItem> selectedItems = selectAllable.getSelectedItems();
            
            if(!isGnutella) {                
                if (selectedItems.size() > 0) {
                    ShareWidget<LocalFileItem[]> shareWidget = shareWidgetFactory.createMultiFileShareWidget();
                    shareWidget.setShareable(selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
                    shareWidget.show(GuiUtils.getMainFrame());
                } else {
                   JPopupMenu popup = new JPopupMenu();
                   popup.add(new JLabel(I18n.tr("Add files to My Library from Tools > Options to share them")));
                   
                   Point mousePoint = getMousePosition(true);
                   if(mousePoint != null) {
                       //move popup 15 pixels to the right so the mouse doesn't obscure the first word
                       popup.show(ShareAllComboBox.this, mousePoint.x + 15, mousePoint.y);
                   }
                }
            } else {
                MultiFileShareModel model = new MultiFileShareModel(shareListManager, selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
                model.shareFriend(SharingTarget.GNUTELLA_SHARE);
            }
        }
    }
    
    /**
	 *	If Gnutella is chosen, unshares all the files in the current table with Gnutella.
	 *  If a friend is selected, opens the Multi-file unshare widget
	 */
    private class UnShareAllAction extends AbstractAction {
        private final boolean isGnutella;
        
        public UnShareAllAction(boolean isGnutella) {
            this.isGnutella = isGnutella;
            
            if(!isGnutella) {
                putValue(Action.NAME, I18n.tr("Unshare all with friend..."));
            } else {
                putValue(Action.NAME, I18n.tr("Unshare all with P2P Network"));
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            SelectAllable<LocalFileItem> selectAllable = myLibraryPanel.getTable();
            selectAllable.selectAll();
            List<LocalFileItem> selectedItems = selectAllable.getSelectedItems();
            
            if(!isGnutella) {                
                if (selectedItems.size() > 0) {
                    ShareWidget<LocalFileItem[]> shareWidget = shareWidgetFactory.createMultiFileUnshareWidget();
                    shareWidget.setShareable(selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
                    shareWidget.show(GuiUtils.getMainFrame());
                } else {
                   JPopupMenu popup = new JPopupMenu();
                   popup.add(new JLabel(I18n.tr("Add files to My Library from Tools > Options to share them")));
                   
                   Point mousePoint = getMousePosition(true);
                   if(mousePoint != null) {
                       //move popup 15 pixels to the right so the mouse doesn't obscure the first word
                       popup.show(ShareAllComboBox.this, mousePoint.x + 15, mousePoint.y);
                   }
                }
            } else {
                MultiFileShareModel model = new MultiFileShareModel(shareListManager, selectedItems.toArray(new LocalFileItem[selectedItems.size()]));
                model.unshareFriend(SharingTarget.GNUTELLA_SHARE);   
            }
        }
    }
    
    /**
     * Opens the signon screen.
     */
    private class SignedOutAction extends AbstractAction {
        public SignedOutAction() {
            putValue(Action.NAME, I18n.tr("Sign in to share with friends"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            friendsSignInPanel.signIn();
        }
    }
}
