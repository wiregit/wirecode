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
import org.limewire.ui.swing.sharing.MockFriendSharingDisplay;
import org.limewire.ui.swing.util.IconManagerStub;
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
                MockFriendSharingDisplay friendSharing = new MockFriendSharingDisplay();
                FriendsPane friendsPane = new FriendsPane(icons, new MockFriendsCountUpdater(), libraryManager, friendSharing);
                frame.add(new ChatPanel(new ConversationPaneFactory() {
                    @Override
                    public ConversationPane create(MessageWriter writer, ChatFriend chatFriend) {
                        return new ConversationPane(writer, chatFriend, libraryManager, new IconManagerStub(), new MockFriendSharingDisplay(), null);
                    }
                }, icons, friendsPane, new TopPanel(icons, friendsPane), friendSharing));
                
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
            new MessageReceivedEvent(new MockMessage(new MockChatFriend(id, "hey", Mode.available), "yo", System.currentTimeMillis(), "me", Type.Received, null)).publish();
        }
    }
}
