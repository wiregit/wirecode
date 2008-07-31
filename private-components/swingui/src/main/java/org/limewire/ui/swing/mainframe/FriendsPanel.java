package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.bushe.swing.event.annotation.AnnotationProcessor;
import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.limewire.ui.swing.friends.DisplayFriendsEvent;

import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc. 
 * FIXME: Had to make this class public so that the EventBus annotation 
 * processor would find annotated methods. :-(
 * TODO: Add Javadocs
 */
@Singleton
public class FriendsPanel extends JXCollapsiblePane {
    public FriendsPanel() {
        super(Direction.UP);
        AnnotationProcessor.process(this);
        setLayout(new GridLayout(10, 1));
        setBorder(BorderFactory.createLineBorder(Color.BLACK));
        for (int i = 0; i < 10; i++) {
            add(new JLabel("This is a stand-in for the content of the friends panel"));
        }
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
        Dimension ps = getPreferredSize();
        int w = (int) ps.getWidth();
        int h = (int) ps.getHeight();
        //FIXME: Hard coded Y subtraction component
        setBounds(parentBounds.width - w, parentBounds.y + parentBounds.height - 204, w, h);
    }

    public void resize() {
        if (!isCollapsed()) {
            resetBounds();
        }
    }
}
