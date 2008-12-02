package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.friend.Friend;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.chat.ChatFriendListPane;
import org.limewire.ui.swing.library.nav.FriendSelectEvent;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
/**
 * Starts a chat with the selected user in the left panel if possible.
 */
public class ChatAction extends AbstractAction {

    private Friend friend;

    final ChatFriendListPane friendsPane;

    final ChatFramePanel friendsPanel;

    @Inject
    ChatAction(
            @Named("friendSelection") ListenerSupport<FriendSelectEvent> friendSelectListenerSupport,
            ChatFriendListPane friendsPane, ChatFramePanel friendsPanel) {
        super(I18n.tr("Chat"));

        this.friendsPane = friendsPane;
        this.friendsPanel = friendsPanel;

        // listen for changes in the selected friend
        friendSelectListenerSupport.addListener(new EventListener<FriendSelectEvent>() {
            @Override
            public void handleEvent(final FriendSelectEvent event) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        setFriend(event.getSelectedFriend());
                    }
                });
            }
        });

        updateDisability();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (friend != null) {
            friendsPanel.setChatPanelVisible(true);
            friendsPane.fireConversationStarted(friend);
        }
    }

    /**
     * Sets a new firend for this action and calls updateDisability().
     */
    private void setFriend(Friend friend) {
        this.friend = friend;
        updateDisability();
    }

    /**
     * Enables or disables the action based on whether the new friend is a
     * jabber user.
     */
    private void updateDisability() {
        if (friend == null || friend.isAnonymous() || !(friend instanceof User)) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }
}