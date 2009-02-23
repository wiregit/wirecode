package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.GnutellaFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.login.FriendsSignInPanel;
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
    
    private JPopupMenu menu = new JPopupMenu();
    
    private AbstractAction shareAllAction;
    private AbstractAction unshareAllAction;
    private AbstractAction shareAllFriendAction;
    private AbstractAction unshareAllFriendAction;
    private AbstractAction signedOutAction;
    
    public ShareAllComboBox(GnutellaFileList gnutellaFileList, XMPPService xmppService, FriendsSignInPanel friendsSignInPanel) {
        this.xmppService = xmppService;
        this.friendsSignInPanel = friendsSignInPanel;
        
        GuiUtils.assignResources(this);
        
        overrideMenu(menu);
        
        createActions(gnutellaFileList);
        
        SharingListener listener = new SharingListener();
        menu.addPopupMenuListener(listener);
    }
    
    private void createActions(GnutellaFileList gnutellaFileList) {
        shareAllAction = new ShareAllAction(gnutellaFileList);
        unshareAllAction = new UnShareAllAction(gnutellaFileList);
        shareAllFriendAction = new ShareAllAction(null);
        unshareAllFriendAction = new UnShareAllAction(null);
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
    
    private class ShareAllAction extends AbstractAction {
        private final GnutellaFileList fileList;
        
        public ShareAllAction(GnutellaFileList fileList) {
            this.fileList = fileList;
            
            if(fileList == null) {
                putValue(Action.NAME, I18n.tr("Share all with friend..."));
            } else {
                putValue(Action.NAME, I18n.tr("Share all with P2P Network"));
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }
    
    private class UnShareAllAction extends AbstractAction {
        private final GnutellaFileList fileList;
        
        public UnShareAllAction(GnutellaFileList fileList) {
            this.fileList = fileList;
            
            if(fileList == null) {
                putValue(Action.NAME, I18n.tr("Unshare all with friend..."));
            } else {
                putValue(Action.NAME, I18n.tr("Unshare all with P2P Network"));
            }
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {

        }
    }
    
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
