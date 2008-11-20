package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendPresence;
import org.limewire.core.api.friend.Network;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendComboBox extends JComboBox implements RegisteringEventListener<RosterEvent> {
    
    private final Map<String, User> friendMap = new ConcurrentHashMap<String, User>();
    private final Map<String, FriendItem> friendItemMap = new ConcurrentHashMap<String, FriendItem>();
     
    private static final String ALL = I18n.tr("All Files");
    private static final String GNUTELLA = I18n.tr("LimeWire Network");
    
    private BaseLibraryMediator basePanel;
    
    @Inject
    public FriendComboBox(final Navigator navigator, final SharingLibraryFactory sharingFactory, final LibraryManager libraryManager, final ShareListManager shareListManager) {
        EventAnnotationProcessor.subscribe(this);
        
        addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                FriendItem friend = (FriendItem) getModel().getSelectedItem();
                if(friend.getId().equals(ALL))
                    return;
                
                if(friend.getId().equals(GNUTELLA)) {
                    JComponent component = sharingFactory.createSharingLibrary(basePanel, new Gnutella(), 
                            libraryManager.getLibraryManagedList().getSwingModel(),
                            shareListManager.getGnutellaShareList());
                    
                    basePanel.setSharingCard(component);
                    basePanel.showSharingCard();
                } else { 
                    JComponent component = sharingFactory.createSharingLibrary(basePanel, friendMap.get(friend.getId()), 
                            libraryManager.getLibraryManagedList().getSwingModel(),
                            shareListManager.getFriendShareList(friendMap.get(friend.getId())));
    
                    basePanel.setSharingCard(component);
                    basePanel.showSharingCard();
                }
            }
        });
        
        loadDefaults();
    }
    
    public void setBasePanel(BaseLibraryMediator basePanel) {
        this.basePanel = basePanel;
    }
    
    private void loadDefaults() {
        addItem(new FriendItem(ALL, ALL));
        addItem(new FriendItem(GNUTELLA, GNUTELLA));
    }
    
    @Override
    @SwingEDTEvent
    public void handleEvent(final RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            if(friendMap.containsKey(event.getSource().getId()))
                return;
            FriendItem item = new FriendItem(event.getSource().getId(), event.getSource().getRenderName());
            addItem(item);
            friendMap.put(event.getSource().getId(), event.getSource());
            friendItemMap.put(event.getSource().getId(), item);
        } else if(event.getType().equals(User.EventType.USER_DELETED)) {
            FriendItem item = friendItemMap.remove(event.getSource().getId());
            friendMap.remove(event.getSource().getId());
            removeItem(item);
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
        }
    }   
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        for(User user : friendMap.values()) {
            FriendItem item = friendItemMap.remove(user.getId());
            friendMap.remove(user.getId());
            removeItem(item);
        }
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }
    
    public void reset() {
        setSelectedIndex(0);
    }
    
    private static class Gnutella implements Friend {
        @Override
        public boolean isAnonymous() {
            return true;
        }
        
        @Override
        public String getId() {
            return "_@_internal_@_";
        }

        @Override
        public String getName() {
            return I18n.tr("LimeWire Network");
        }
        
        @Override
        public String getRenderName() {
            return getName();
        }

        public void setName(String name) {
            
        }

        @Override
        public Network getNetwork() {
            return null;
        }

        @Override
        public Map<String, FriendPresence> getFriendPresences() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
    
    private class FriendItem {
        private final String id;
        private final String displayName;
        
        public FriendItem(String id, String displayName) {
            this.id = id;
            this.displayName = displayName;
        }
        
        public String getId() {
            return id;
        }
        
        public String toString() {
            return displayName;
        }
    }
}
