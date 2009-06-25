package org.limewire.ui.swing.friends;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import org.limewire.friend.api.FriendRequestEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class FriendRequestNotificationController implements ComponentListener {

    
        private FriendRequestNotificationPanel currentPanel = null;

        private final Provider<FriendRequestNotificationPanel> friendRequestNotifiactionPanelProvider;

        @Inject
        public FriendRequestNotificationController(
                Provider<FriendRequestNotificationPanel> friendRequestNotifiactionPanelProvider) {
            this.friendRequestNotifiactionPanelProvider = friendRequestNotifiactionPanelProvider;
        }

        @Inject
        public void register(ListenerSupport<FriendRequestEvent> friendRequestListeners) {
            friendRequestListeners.addListener(new EventListener<FriendRequestEvent>() {
                @Override
                @SwingEDTEvent
                public void handleEvent(FriendRequestEvent event) {
                    if (currentPanel == null) {
                        currentPanel = friendRequestNotifiactionPanelProvider.get();
                        // component hidden event comes in to tell us we can show more
                        // warnings.
                        currentPanel.addComponentListener(FriendRequestNotificationController.this);
                    }
                    
                    currentPanel.addRequest(event.getData());
                }
            });
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            currentPanel = null;
        }

        @Override
        public void componentMoved(ComponentEvent e) {

        }

        @Override
        public void componentResized(ComponentEvent e) {

        }

        @Override
        public void componentShown(ComponentEvent e) {

        }

    }

