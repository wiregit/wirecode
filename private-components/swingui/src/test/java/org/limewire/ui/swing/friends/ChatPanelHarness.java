package org.limewire.ui.swing.friends;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.limewire.core.impl.library.MockLibraryManager;
import org.limewire.ui.swing.nav.MockNavigableTree;
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
                FriendsPane friendsPane = new FriendsPane(icons, new MockFriendsCountUpdater(), libraryManager);
                frame.add(new ChatPanel(new ConversationPaneFactory() {
                    @Override
                    public ConversationPane create(MessageWriter writer, Friend friend) {
                        return new ConversationPane(writer, friend);
                    }
                }, icons, friendsPane, new TopPanel(icons, friendsPane), new MockNavigableTree()));
                
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
        return builder.getPanel();
    }
}
