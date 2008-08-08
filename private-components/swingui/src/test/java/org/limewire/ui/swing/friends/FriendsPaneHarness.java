package org.limewire.ui.swing.friends;

import java.awt.Dimension;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

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

                FriendsPane pane = new FriendsPane();
                frame.add(pane);

                final ArrayList<FriendImpl> friends = new ArrayList<FriendImpl>();
                for (int i = 0; i < 10; i++) {
                    FriendImpl friend = new FriendImpl("foo" + i, "Sort-in", randomMode());
                    pane.addFriend(friend);
                    friends.add(friend);
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            FriendImpl friend = friends.get((int) (Math.random() * 10));
                            friend.setMode(randomMode());
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }).start();

                frame.setPreferredSize(new Dimension(470, 400));

                frame.pack();
                frame.setVisible(true);
            }
        });
    }

    private static Mode randomMode() {
        int val = (int) (Math.random() * 10);
        return modes[val / 2];
    }
}
