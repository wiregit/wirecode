package com.limegroup.gnutella.privategroups;

import org.limewire.listener.Event;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.privategroups.RosterListMediator;

/**
 * A class that listens for events that request to open a new chat window
 */
public class ChatWindowListener implements EventListener{

    public void handleEvent(Event event) {
        if(event instanceof ChatWindowEvent){
            //start new message window
            ChatWindowEvent chatEvent = (ChatWindowEvent) event;
            RosterListMediator.getInstance().initMessageWindow(chatEvent.getRemoteUser(), chatEvent.getLocalUser());
            
            //need to remove self from buddyListManager window listeners
            //or else a new window will be generated each time a ChatWindowListener event is broadcasted
            GuiCoreMediator.getPGRPClient().getBuddyListManager().removeListener(GuiCoreMediator.getPGRPClient().getBuddyListManager(), this);  
        }
    }
}
