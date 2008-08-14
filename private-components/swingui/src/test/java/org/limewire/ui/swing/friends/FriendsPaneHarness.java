package org.limewire.ui.swing.friends;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.limewire.ui.swing.xmpp.PresenceUpdateEvent;
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

                FriendsPane pane = new FriendsPane(new IconLibraryImpl());
                frame.add(pane);

                final ArrayList<Duo> presences = new ArrayList<Duo>();
                for (int i = 0; i < 10; i++) {
                    MockUser user = new MockUser("", "foo" + i);
                    MockPresence presence = new MockPresence(randomMode(), "Sort-in", "jid" + i);
                    new PresenceUpdateEvent(user, presence).publish();
                    presences.add(new Duo(user, presence));
                }

                frame.setPreferredSize(new Dimension(470, 400));

                frame.pack();
                frame.setVisible(true);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            Duo duo = presences.get((int) (Math.random() * 10));
                            duo.presence.setMode(randomMode());
                            new PresenceUpdateEvent(duo.user, duo.presence).publish();
                            try {
                                Thread.sleep(1000);
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
        private MockUser user;
        private MockPresence presence;
        public Duo(MockUser user, MockPresence presence) {
            super();
            this.presence = presence;
            this.user = user;
        }
    }

    private static Mode randomMode() {
        int val = (int) (Math.random() * 10);
        Mode mode = modes[val / 2];
        return mode == null ? modes[0] : mode;
    }
}
