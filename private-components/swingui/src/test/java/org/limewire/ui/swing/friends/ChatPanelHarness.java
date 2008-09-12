package org.limewire.ui.swing.friends;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.limewire.core.impl.library.MockLibraryManager;
import org.limewire.ui.swing.friends.Message.Type;
import org.limewire.ui.swing.sharing.MockBuddySharingDisplay;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence.Mode;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class ChatPanelHarness {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame();
                final IconLibraryImpl icons = new IconLibraryImpl();
                final MockLibraryManager libraryManager = new MockLibraryManager();
                MockBuddySharingDisplay buddySharing = new MockBuddySharingDisplay();
                FriendsPane friendsPane = new FriendsPane(icons, new MockFriendsCountUpdater(), libraryManager, buddySharing);
                frame.add(new ChatPanel(new ConversationPaneFactory() {
                    @Override
                    public ConversationPane create(MessageWriter writer, Friend friend) {
                        return new ConversationPane(writer, friend);
                    }
                }, icons, friendsPane, new TopPanel(icons, friendsPane), buddySharing));
                
                frame.pack();
                frame.setVisible(true);
                
                JFrame frame2 = new JFrame();
                frame2.add(addFriendPanel());
                frame2.pack();
                frame2.setVisible(true);
            }
        });
    }
    
    private static JPanel addFriendPanel() {
        FormLayout layout = new FormLayout("p, 2dlu, p");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        
        builder.append("Id:");
        final JTextField idField = new JTextField(20);
        builder.append(idField);
        builder.nextLine();
        builder.append("Name:");
        final JTextField nameField = new JTextField(20);
        builder.append(nameField);
        builder.nextLine();
        builder.append("Mood:");
        final JTextField moodField = new JTextField(20);
        builder.append(moodField);
        builder.nextLine();
        JButton addFriend = new JButton("Add Friend");
        addFriend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new PresenceUpdateEvent(new MockUser(idField.getText(), nameField.getText()), new MockPresence(Mode.available, moodField.getText(), idField.getText())).publish();
            }
        });
        builder.append(addFriend, 3);
        JButton fillWithMessageBuddies = new JButton("Fill with Message Awaiting Buddies");
        fillWithMessageBuddies.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fillWithUnseenMessageAwaitingFriends();
            }
        });
        builder.nextLine();
        builder.append(fillWithMessageBuddies, 3);
        return builder.getPanel();
    }
    
    private static void fillWithUnseenMessageAwaitingFriends() {
        for(int i = 0; i < 50; i++) {
            String id = "foo" + i;
            new PresenceUpdateEvent(new MockUser(id, "foo"), new MockPresence(Mode.available, "hey", id)).publish();
        }
        
        for(int i = 0; i < 50; i++) {
            String id = "foo" + i;
            new MessageReceivedEvent(new MockMessage(new MockFriend(id, "hey", Mode.available), "yo", System.currentTimeMillis(), "me", Type.Received)).publish();
        }
    }
}
