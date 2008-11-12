package org.limewire.ui.swing.library;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.core.api.friend.Friend;
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

public class FriendComboBox extends JComboBox implements RegisteringEventListener<RosterEvent> {
    
    private final Map<String, User> friendMap = new ConcurrentHashMap<String, User>();
    
    private static final String ALL = I18n.tr("All Files");
    private static final String GNUTELLA = I18n.tr("LimeWire Network");
    
    private BaseLibraryPanel basePanel;
    
    @Inject
    public FriendComboBox(final Navigator navigator, final SharingLibraryFactory sharingFactory, final LibraryManager libraryManager, final ShareListManager shareListManager) {
        EventAnnotationProcessor.subscribe(this);
        
        addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                String friend = (String) getModel().getSelectedItem();
                if(friend.equals(ALL))
                    return;
                
                if(friend.equals(GNUTELLA)) {
                    JComponent component = sharingFactory.createSharingLibrary(basePanel, new Gnutella(), 
                            libraryManager.getLibraryManagedList().getSwingModel(),
                            shareListManager.getGnutellaShareList());
                    
                    basePanel.setAuxCard(component);
                    basePanel.showAuxCard();
                } else { 
                    JComponent component = sharingFactory.createSharingLibrary(basePanel, friendMap.get(friend), 
                            libraryManager.getLibraryManagedList().getSwingModel(),
                            shareListManager.getFriendShareList(friendMap.get(friend)));
    
                    basePanel.setAuxCard(component);
                    basePanel.showAuxCard();
                }
            }
        });
        
        loadDefaults();
    }
    
    public void setBasePanel(BaseLibraryPanel basePanel) {
        this.basePanel = basePanel;
    }
    
    private void loadDefaults() {
        ((DefaultComboBoxModel)getModel()).addElement(ALL);
        ((DefaultComboBoxModel)getModel()).addElement(GNUTELLA);
    }
    
    @Override
    @SwingEDTEvent
    public void handleEvent(final RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {
            if(friendMap.containsKey(event.getSource().getId()))
                return;
            ((DefaultComboBoxModel)getModel()).addElement(event.getSource().getId());
            friendMap.put(event.getSource().getId(), event.getSource());
        } else if(event.getType().equals(User.EventType.USER_REMOVED)) {
            ((DefaultComboBoxModel)getModel()).removeElement(event.getSource().getId());
            friendMap.remove(event.getSource().getId());
        } else if(event.getType().equals(User.EventType.USER_UPDATED)) {
        }
    }   
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        for(User user : friendMap.values()) {
            ((DefaultComboBoxModel)getModel()).removeElement(user.getId());
            friendMap.remove(user.getId());
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
            return false;
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
    }
}
