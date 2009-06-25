package org.limewire.ui.swing.friends;

import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JLayeredPane;

import org.limewire.friend.api.FriendRequest;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.mainframe.GlobalLayeredPane;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;

public class FriendRequestNotificationPanel extends Panel implements Resizable, ComponentListener {
    // heavy weight so it can be on top of other heavy weight components

    private final JLayeredPane layeredPane;
    private final FriendRequestPanel friendRequestPanel;

    @Inject
    public FriendRequestNotificationPanel(
            @GlobalLayeredPane JLayeredPane layeredPane,
            FriendRequestPanel friendRequestPanel) {
        
        this.layeredPane = layeredPane;
        this.friendRequestPanel = friendRequestPanel;
        
        ResizeUtils.forceSize(friendRequestPanel, new Dimension(300, 100));
           
        this.add(friendRequestPanel);

        friendRequestPanel.addComponentListener(new ComponentListener() {
            @Override
            public void componentHidden(ComponentEvent e) {
                cleanup();                
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
        });
        
        layeredPane.add(this, JLayeredPane.MODAL_LAYER);
        layeredPane.addComponentListener(this);
        resize();
    }

    public void addRequest(FriendRequest request) {
        friendRequestPanel.addRequest(request);
    }
    
    private void cleanup() {
        //firing a component hidden event so that the DocumentWarningController can know when it is ok to show another message.
        for(ComponentListener componentListener : getComponentListeners()) {
            componentListener.componentHidden(new ComponentEvent(this, ComponentEvent.COMPONENT_HIDDEN));
        }
        layeredPane.removeComponentListener(this);
        layeredPane.remove(this);
    }

    @Override
    public void resize() {
        Rectangle parentBounds = layeredPane.getBounds();
        int w = friendRequestPanel.getPreferredSize().width;
        int h = friendRequestPanel.getPreferredSize().height;
        setBounds(parentBounds.width - w, parentBounds.height - h, w, h);
    }

    @Override
    public void componentHidden(ComponentEvent e) {

    }

    @Override
    public void componentMoved(ComponentEvent e) {

    }

    @Override
    public void componentResized(ComponentEvent e) {
        resize();
    }

    @Override
    public void componentShown(ComponentEvent e) {

    }
}
