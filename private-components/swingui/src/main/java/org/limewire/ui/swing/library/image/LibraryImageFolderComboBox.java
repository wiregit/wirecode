package org.limewire.ui.swing.library.image;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.library.SelectAllable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;

public class LibraryImageFolderComboBox extends LimeComboBox {

    @Resource
    private Color labelColor;
    @Resource
    private Font labelFont;
    @Resource
    private Color disabledColor;
    
    private final XMPPService xmppService;
    private final FriendActions friendActions;
//    private final Provider<SharingActionFactory> sharingActionFactoryProvider;
    
    private JPopupMenu menu = new JPopupMenu();
    
    private SelectAllable<LocalFileItem> selectAllable;
    
    @Inject
    public LibraryImageFolderComboBox(XMPPService xmppService, //Provider<SharingActionFactory> sharingActionFactory,
            FriendActions friendActions) {
        this.xmppService = xmppService;
        this.friendActions = friendActions;
//        this.sharingActionFactoryProvider = sharingActionFactory;
                
        GuiUtils.assignResources(this);
        
        overrideMenu(menu);
        
        SharingListener listener = new SharingListener();
        menu.addPopupMenuListener(listener);
    }
    
    public void setSelectAllable(SelectAllable<LocalFileItem> selectAllable) {
        this.selectAllable = selectAllable;
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
            
            if(selectAllable == null)
                return;
            
//            SharingActionFactory sharingActionFactory = sharingActionFactoryProvider.get();
//            
//            // if not logged in don't show options for friends.
//            if(!xmppService.isLoggedIn()) {
//                menu.add(decorateItem(sharingActionFactory.createShareGnutellaAction(true, selectAllable)));
//                menu.add(decorateItem(sharingActionFactory.createUnshareGnutellaAction(true, selectAllable)));
//                
//                menu.addSeparator();
//                
//                menu.add(decorateDisabledfItem(new DisabledFriendLoginAction(I18n.tr("Sign in to share with friends"), friendActions)));
//            } else {
//                menu.add(decorateItem(sharingActionFactory.createShareGnutellaAction(true, selectAllable)));
//                menu.add(decorateItem(sharingActionFactory.createShareFriendAction(true, selectAllable)));
//                
//                menu.addSeparator();
//                
//                menu.add(decorateItem(sharingActionFactory.createUnshareGnutellaAction(true, selectAllable)));
//                menu.add(decorateItem(sharingActionFactory.createUnshareFriendAction(true, selectAllable)));
//            }
        }
    }
}
