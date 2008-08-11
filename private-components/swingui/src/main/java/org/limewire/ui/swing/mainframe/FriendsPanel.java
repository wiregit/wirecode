package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;

import javax.swing.BorderFactory;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.limewire.ui.swing.friends.DisplayFriendsEvent;
import org.limewire.ui.swing.friends.LoginPanel;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc. 
 * FIXME: Had to make this class public so that the EventBus annotation 
 * processor would find annotated methods. :-(
 * TODO: Add Javadocs
 */
@Singleton
public class FriendsPanel extends JXCollapsiblePane {
    private final LoginPanel loginPanel;
    
    @Inject
    public FriendsPanel(XMPPService xmppService) {
        super(Direction.UP);
        AnnotationProcessor.process(this);
        setLayout(new GridLayout(10, 1));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        loginPanel = new LoginPanel(xmppService);
        add(loginPanel);
        setCollapsed(true);
    }

    @EventSubscriber
    public void handleAppear(DisplayFriendsEvent event) {
        if (isCollapsed()) {
            resetBounds();
        }
        // toggle
        setCollapsed(!isCollapsed());
    }
    
    private void resetBounds() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = loginPanel.getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds(parentBounds.width - w, parentBounds.height - h, w, h);
    }

    public void resize() {
        if (!isCollapsed()) {
            resetBounds();
        }
    }
}
