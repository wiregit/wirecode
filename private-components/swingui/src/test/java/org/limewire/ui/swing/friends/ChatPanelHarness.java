package org.limewire.ui.swing.friends;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.impl.library.MockLibraryManager;
import org.limewire.listener.EventListenerList;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.ui.swing.sharing.MockFriendSharingDisplay;
import org.limewire.ui.swing.util.IconManagerStub;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.Presence.Mode;

public class ChatPanelHarness {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();
                final IconLibraryImpl icons = new IconLibraryImpl();
                final MockLibraryManager libraryManager = new MockLibraryManager();
                MockFriendSharingDisplay friendSharing = new MockFriendSharingDisplay();
                EventListenerList<FriendPresenceEvent> presenceSupport = new EventListenerList<FriendPresenceEvent>();
                final EventListenerList<FriendEvent> friendSupport = new EventListenerList<FriendEvent>();
                FriendsPane friendsPane = new FriendsPane(icons, new MockFriendsCountUpdater(), libraryManager, friendSharing, presenceSupport);
                frame.add(new ChatPanel(new ConversationPaneFactory() {
                    @Override
                    public ConversationPane create(MessageWriter writer, ChatFriend chatFriend, String loggedInID) {
                        return new ConversationPane(writer, chatFriend, loggedInID, libraryManager, new IconManagerStub(), new MockFriendSharingDisplay(), null, null, friendSupport);
                    }
                }, icons, friendsPane, new TopPanel(icons, null), friendSharing));
                
                frame.pack();
                frame.setVisible(true);
                
                JFrame frame2 = new JFrame();
                frame2.add(addFriendPanel(presenceSupport, friendSupport));
                frame2.pack();
                frame2.setVisible(true);
            }
        });
    }
    
    private static JPanel addFriendPanel(final EventListenerList<FriendPresenceEvent> presenceSupport, 
                                         final EventListenerList<FriendEvent> friendSupport) {
        JPanel panel = new JPanel(new MigLayout("", "[][]", ""));
        
        panel.add(new JLabel("Id:"));
        final JTextField idField = new JTextField(20);
        panel.add(idField, "wrap");
        panel.add(new JLabel("Name:"));
        final JTextField nameField = new JTextField(20);
        panel.add(nameField, "wrap");
        panel.add(new JLabel("Mood:"));
        final JTextField moodField = new JTextField(20);
        panel.add(moodField, "wrap");
        JButton addFriend = new JButton("Add Friend");
        addFriend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                User user = new MockUser(idField.getText(), nameField.getText());
                friendSupport.broadcast(new FriendEvent(user, FriendEvent.Type.ADDED));
                presenceSupport.broadcast(new FriendPresenceEvent(
                        new MockPresence(user, Mode.available, moodField.getText(), idField.getText()), FriendPresenceEvent.Type.ADDED));
            }
        });
        panel.add(addFriend, "span, wrap");
        JButton fillWithMessageBuddies = new JButton("Fill with Message Awaiting Buddies");
        fillWithMessageBuddies.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fillWithUnseenMessageAwaitingFriends(presenceSupport, friendSupport);
            }
        });
        panel.add(fillWithMessageBuddies, "span");
        return panel;
    }
    
    private static void fillWithUnseenMessageAwaitingFriends(
            final EventListenerList<FriendPresenceEvent> presenceSupport, 
            final EventListenerList<FriendEvent> friendSupport) {
        for(int i = 0; i < 50; i++) {
            String id = "foo" + i;
            User user = new MockUser(id, "foo");
            friendSupport.broadcast(new FriendEvent(user, FriendEvent.Type.ADDED));
            presenceSupport.broadcast(new FriendPresenceEvent(
                    new MockPresence(user, Mode.available,"hey", id), FriendPresenceEvent.Type.ADDED));
        }
        
        for(int i = 0; i < 50; i++) {
            String id = "foo" + i;
            new MessageReceivedEvent(new MockMessage(new MockChatFriend(id, "hey", Mode.available), "yo", System.currentTimeMillis(), "me", Type.Received)).publish();
        }
    }
}
