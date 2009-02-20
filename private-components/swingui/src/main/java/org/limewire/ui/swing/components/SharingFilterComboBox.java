package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.SharingMatchingEditor;
import org.limewire.ui.swing.library.sharing.SharingTarget;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.google.inject.name.Named;

public class SharingFilterComboBox extends LimeComboBox {

    private final Map<Friend, MenuAction> menuMap = new HashMap<Friend, MenuAction>();
    
    private final SharingMatchingEditor matchingEditor;
    
    private final ShareListManager shareListManager;
    
    @AssistedInject
    public SharingFilterComboBox(@Assisted SharingMatchingEditor matchingEditor, @Assisted ShareListManager shareListManager) {
        this.matchingEditor = matchingEditor;
        this.shareListManager = shareListManager;
        
        addFriend(new FullLibrary());
        addFriend(SharingTarget.GNUTELLA_SHARE.getFriend());
    }
    
    public void addFriend(Friend friend) {
        MenuAction menuAction = new MenuAction(friend);
        menuMap.put(friend, menuAction);
        addAction(menuAction);
    }
    
    public void removeFriend(Friend friend) {
        MenuAction action = menuMap.remove(friend);
        removeAction(action);
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
            if(friend.getId() == null) 
                matchingEditor.setFriendList(null);
            else if(friend.getId().equals(Friend.P2P_FRIEND_ID))
                matchingEditor.setFriendList(shareListManager.getGnutellaShareList().getSwingModel());
            else
                matchingEditor.setFriendList(shareListManager.getFriendShareList(friend).getSwingModel());
        }
        
        public String toString() {
            return friend.getRenderName();
        }
    }
    
    private class FullLibrary implements Friend {

        @Override
        public String getFirstName() {return null;}
        @Override
        public Map<String, FriendPresence> getFriendPresences() {return null;}
        @Override
        public String getId() {return null;}
        @Override
        public String getName() {return null;}
        @Override
        public Network getNetwork() {return null;}
        @Override
        public boolean isAnonymous() {return false;}
        @Override
        public void setName(String name) {}

        @Override
        public String getRenderName() {
            return I18n.tr("Sharing With...");
        }
    }
}
