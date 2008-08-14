package org.limewire.ui.swing.friends;

import java.util.ArrayList;
import java.util.Collections;

import org.limewire.xmpp.api.client.Presence.Mode;

import junit.framework.TestCase;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class FriendAvailabilityComparatorTest extends TestCase {

    private ArrayList<Friend> friends;
    
    public void setUp() {
        friends = new ArrayList<Friend>();
    }
    
    private void populateFriends(String name, Mode mode) {
        friends.add(new MockFriend("", name, "foo", mode));
    }

    public void testModeAlphabeticalSorting() {
        populateFriends("b_xa", Mode.xa);
        populateFriends("b_available", Mode.available);
        populateFriends("b_away", Mode.away);
        populateFriends("b_dnd", Mode.dnd);
        populateFriends("A_away", Mode.away);
        populateFriends("a_xa", Mode.xa);
        populateFriends("b_chat", Mode.chat);
        populateFriends("a_dnd", Mode.dnd);
        populateFriends("a_available", Mode.available);
        
        Collections.sort(friends, new FriendAvailabilityComparator());
        
        String[] sorted = new String[] {"b_chat", 
                                        "a_available", "b_available",
                                        "A_away", "b_away",
                                        "a_xa", "b_xa",
                                        "a_dnd", "b_dnd"};
        
        for(int i = 0; i < sorted.length; i++) {
            assertEquals(sorted[i], friends.get(i).getName());
        }
    }

    public void testHandleNullFriendName() {
        populateFriends("a_xa", Mode.xa);
        populateFriends(null, Mode.xa);
        populateFriends(null, Mode.chat);
        populateFriends("a_chat", Mode.chat);
        
        Collections.sort(friends, new FriendAvailabilityComparator());
        
        String[] sorted = new String[] {"a_chat", null, "a_xa", null};
        
        for(int i = 0; i < sorted.length; i++) {
            assertEquals(sorted[i], friends.get(i).getName());
        }
    }
    
//    public void testOrderSortingForChatStatus() {
//        friends.add(new MockFriend("b_chat", "foo", Mode.chat));
//        friends.add(new MockFriend("a_chat", "foo", Mode.chat));
//        
//        Collections.sort(friends, new FriendAvailabilityComparator());
//
//        
//    }
}
