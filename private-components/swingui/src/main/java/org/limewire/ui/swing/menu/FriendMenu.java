package org.limewire.ui.swing.menu;

import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventUtils;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.friends.actions.AddFriendAction;
import org.limewire.ui.swing.friends.actions.BrowseFriendsAction;
import org.limewire.ui.swing.friends.actions.SignInOutAction;
import org.limewire.ui.swing.friends.actions.StatusActions;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

class FriendMenu extends MnemonicMenu {

    private final BrowseFriendsAction browseFriendAction;
    private final StatusActions statusActions;
    private final SignInOutAction signInOutAction;
    private final AddFriendAction addFriendAction;

    @Inject
    public FriendMenu(EventBean<FriendConnectionEvent> friendConnectionEventBean,
            BrowseFriendsAction browseFriendAction, StatusActions statusActions,
            SignInOutAction signInOutAction, AddFriendAction addFriendAction) {
        super(I18n.tr("&Friends"));
        this.browseFriendAction = browseFriendAction;
        this.statusActions = statusActions;
        this.signInOutAction = signInOutAction;
        this.addFriendAction = addFriendAction;

        add(browseFriendAction);
        addSeparator();
        add(addFriendAction);
        addSeparator();
        add(statusActions.getAvailableMenuItem());
        add(statusActions.getDnDMenuItem());
        addSeparator();
        add(signInOutAction);

        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        updateSignedInStatus(friendConnection != null && friendConnection.isLoggedIn());
    }

    @Inject
    void register(ListenerSupport<FriendConnectionEvent> event) {
        event.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                switch (event.getType()) {
                case CONNECTED:
                case CONNECTING:
                case CONNECT_FAILED:
                case DISCONNECTED:
                    FriendConnection connection = event.getSource();
                    if (connection != null && connection.isLoggedIn()) {
                        updateSignedInStatus(true);
                    } else {
                        updateSignedInStatus(false);
                    }
                    break;
                }
            }
        });
    }

    private void updateSignedInStatus(boolean signedIn) {
        //TODO probably disable login action while logging in.
        browseFriendAction.setEnabled(signedIn);
        addFriendAction.setEnabled(signedIn);
        statusActions.updateSignedInStatus();
        signInOutAction.updateSignedInStatus(signedIn);
    }

}
