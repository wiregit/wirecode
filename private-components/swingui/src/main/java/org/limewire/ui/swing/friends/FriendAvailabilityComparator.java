package org.limewire.ui.swing.friends;

import static org.limewire.xmpp.api.client.Presence.Mode.available;
import static org.limewire.xmpp.api.client.Presence.Mode.away;
import static org.limewire.xmpp.api.client.Presence.Mode.chat;
import static org.limewire.xmpp.api.client.Presence.Mode.dnd;
import static org.limewire.xmpp.api.client.Presence.Mode.xa;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.limewire.xmpp.api.client.Presence.Mode;

class FriendAvailabilityComparator implements Comparator<Friend> {
    private static ArrayList<Mode> ORDERED = new ArrayList<Mode>(Arrays.asList(new Mode[]{chat, available, away, xa, dnd})); 
    
    @Override
    public int compare(Friend a, Friend b) {
        boolean a_chatting = a.isChatting();
        boolean b_chatting = b.isChatting();
        if(a_chatting && b_chatting == false) {
            return -1;
        } else if (b_chatting && a_chatting == false) {
            return 1;
        } else if (a_chatting && b_chatting) {
            return new Long(a.getChatStartTime()).compareTo(new Long(b.getChatStartTime()));
        }
        
        int a_mode_index = order(a);
        int b_mode_index = order(b);
        if (a_mode_index > b_mode_index) {
            return 1;
        } else if (b_mode_index > a_mode_index) {
            return -1;
        }
        String a_name = a.getName();
        String b_name = b.getName();
        if (a_name == null) {
            return 1;
        }
        if (b_name == null) {
            return -1;
        }
        return a_name.compareTo(b_name);
    }
    
    private static int order(Friend friend) {
        return ORDERED.indexOf(friend.getMode());
    }
}