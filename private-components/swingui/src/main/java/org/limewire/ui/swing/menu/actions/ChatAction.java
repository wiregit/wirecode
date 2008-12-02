package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.friend.Friend;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.nav.FriendSelectEvent;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.User;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class ChatAction extends AbstractAction {

    private Friend friend;

    @Inject
    ChatAction(
            @Named("friendSelection") ListenerSupport<FriendSelectEvent> friendSelectListenerSupport) {
        super(I18n.tr("Chat"));
        update();
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
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("Chat with: " + friend);
    }

    public void setFriend(Friend friend) {
        this.friend = friend;
        update();
    }

    private void update() {
        if (friend == null || friend.isAnonymous() || !(friend instanceof User)) {
            setEnabled(false);
        } else {
            setEnabled(true);
        }
    }
}