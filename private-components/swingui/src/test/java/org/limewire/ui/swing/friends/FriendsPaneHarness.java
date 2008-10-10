package org.limewire.ui.swing.friends;

import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.limewire.core.impl.library.MockLibraryManager;
import org.limewire.ui.swing.sharing.MockFriendSharingDisplay;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 * @author Mario Aquino, Object Computing, Inc.
 * 
 */
public class FriendsPaneHarness {
    private static final Mode[] modes = Mode.values();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();

                FriendsPane pane = new FriendsPane(new IconLibraryImpl(), new MockFriendsCountUpdater(), new MockLibraryManager(), new MockFriendSharingDisplay());
                frame.add(pane);

                final ArrayList<Duo> presences = new ArrayList<Duo>();
                String[] names = new String[] { "lmfiney", "kristim", "lare77", "marioaquino", "natenff", 
                        "moe", "larry", "curly", "shemp", "curly-joe"};
                int i = 0;
                for(String name : names) {
                    MockUser user = new MockUser("", name);
                    MockPresence presence = new MockPresence(user, randomMode(), "Sort-in", "jid" + i++);
                    new PresenceUpdateEvent(presence).publish();
                    presences.add(new Duo(presence));
                }

                frame.pack();
                frame.setVisible(true);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            Duo duo = presences.get(get1to10());
                            duo.presence.setMode(randomMode());
                            new PresenceUpdateEvent(duo.presence).publish();
                            try {
                                Thread.sleep(10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();
            }
        });
    }
    
    private static class Duo {
        private MockPresence presence;
        public Duo(MockPresence presence) {
            super();
            this.presence = presence;
        }
    }

    private static Mode randomMode() {
        Mode mode = modes[get1to10() / 2];
        return mode == null ? modes[0] : mode;
    }

    private static int get1to10() {
        return (int) (Math.random() * 10);
    }
}
