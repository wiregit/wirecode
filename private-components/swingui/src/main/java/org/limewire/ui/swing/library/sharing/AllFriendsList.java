package org.limewire.ui.swing.library.sharing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class AllFriendsList implements RegisteringEventListener<RosterEvent> {
    
    private final List<SharingTarget> allFriends = Collections.synchronizedList(new ArrayList<SharingTarget>());
    
    @Inject
    public AllFriendsList(){
        EventAnnotationProcessor.subscribe(this);
    }
    
    public List<SharingTarget> getAllFriends(){
        return allFriends;
    }

    @Override
    public void handleEvent(final RosterEvent event) {
        if(event.getType().equals(User.EventType.USER_ADDED)) {              
            allFriends.add(new SharingTarget(event.getSource()));
        } else if(event.getType().equals(User.EventType.USER_DELETED)) {
            allFriends.remove(new SharingTarget(event.getSource()));
        }
    }   
    
    @EventSubscriber
    public void handleSignoff(SignoffEvent event) {
        allFriends.clear();
    }

    @Override
    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }
}
