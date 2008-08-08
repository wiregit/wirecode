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
        int a_mode_index = order(a);
        int b_mode_index = order(b);
        if (a_mode_index > b_mode_index) {
            return 1;
        } else if (b_mode_index > a_mode_index) {
            return -1;
        }
        return a.getName().compareTo(b.getName());
    }
    
    private static int order(Friend friend) {
        return ORDERED.indexOf(friend.getMode());
    }
}