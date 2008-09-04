package org.limewire.ui.swing.mainframe;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.border.Border;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.jdesktop.swingx.JXCollapsiblePane;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.event.PanelDisplayedEvent;
import org.limewire.ui.swing.friends.ChatPanel;
import org.limewire.ui.swing.friends.DisplayFriendsEvent;
import org.limewire.ui.swing.friends.DisplayFriendsToggleEvent;
import org.limewire.ui.swing.friends.Displayable;
import org.limewire.ui.swing.friends.LoginPanel;
import org.limewire.ui.swing.friends.SignoffEvent;
import org.limewire.ui.swing.friends.XMPPConnectionEstablishedEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * @author Mario Aquino, Object Computing, Inc. 
 * TODO: Add Javadocs
 */
@Singleton
public class FriendsPanel extends JXCollapsiblePane implements Resizable{
    private final LoginPanel loginPanel;
    private final ChatPanel chatPanel;
    //Heavy-weight component so that it can appear above other heavy-weight components
    private final Panel mainPanel;
    
    @Inject
    public FriendsPanel(LoginPanel loginPanel, ChatPanel chatPanel) {
        super(Direction.UP, new BorderLayout());
        this.chatPanel = chatPanel;
        this.loginPanel = loginPanel;
        this.mainPanel = new Panel();
        
        mainPanel.setVisible(false);
        mainPanel.setBackground(getBackground());

        Border lineBorder = BorderFactory.createLineBorder(Color.BLACK);
        chatPanel.setBorder(lineBorder);
        loginPanel.setBorder(lineBorder);
        mainPanel.add(loginPanel);
        add(mainPanel);
        setCollapsed(true);
        
        EventAnnotationProcessor.subscribe(this);
    }

    @EventSubscriber
    public void handleAppear(DisplayFriendsEvent event) {
        displayFriendsPanel(event.shouldShow());
    }

    @EventSubscriber
    public void handleAppear(DisplayFriendsToggleEvent event) {
        displayFriendsPanel(isCollapsed());
    }

    private void displayFriendsPanel(boolean shouldDisplay) {
        if (shouldDisplay) {
            resetBounds();
        }
        //If it should display, should not be collapsed and vice versa
        setCollapsed(!shouldDisplay);
        mainPanel.setVisible(!isCollapsed());
        if (!isCollapsed()) {
            ((Displayable)mainPanel.getComponent(0)).handleDisplay();
            new PanelDisplayedEvent(this).publish();
        }
    }
    
    /**
     * Hides FriendsPanel when another panel is shown in the same layer.
     */
    @EventSubscriber
    public void handleOtherPanelDisplayed(PanelDisplayedEvent event){
        if(event.getDisplayedPanel() != this){
            setCollapsed(true);
            mainPanel.setVisible(false);
        }
    }
    
    @EventSubscriber
    public void handleConnectionEstablished(XMPPConnectionEstablishedEvent event) {
        mainPanel.remove(loginPanel);
        mainPanel.add(chatPanel);
        chatPanel.setLoggedInID(event.getID());
        resetBounds();
    }
    
    @EventSubscriber
    public void handleLogoffEvent(SignoffEvent event) {
        mainPanel.remove(chatPanel);
        mainPanel.add(loginPanel);
        resetBounds();
        displayFriendsPanel(false);
    }
    
    private void resetBounds() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = mainPanel.getPreferredSize();
        int w = (int) childPreferredSize.getWidth();
        int h = (int) childPreferredSize.getHeight();
        setBounds(parentBounds.width - w, parentBounds.height - h, w, h);
    }

    @Override
    public void resize() {
        if (!isCollapsed()) {
            resetBounds();
        }
    }
}
