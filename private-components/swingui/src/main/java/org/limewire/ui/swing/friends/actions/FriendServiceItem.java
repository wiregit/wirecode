package org.limewire.ui.swing.friends.actions;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.jdesktop.application.Resource;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventUtils;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class FriendServiceItem extends JLabel {

    @Resource private Font font;
    @Resource private Color foreground;
    @Resource private Color background;
    @Resource private Color separatorForeground;
    
    @Inject
    public FriendServiceItem(EventBean<FriendConnectionEvent> friendConnectionEventBean) {
        
        GuiUtils.assignResources(this);
        
        FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
        if (friendConnection != null && friendConnection.isLoggedIn()) {
            setFont(font);
            setForeground(foreground);
            setBackground(background);
            setOpaque(true);
            
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0, separatorForeground), 
                    BorderFactory.createEmptyBorder(0,10,0,0)));
            setIconTextGap(4);
            
            setMaximumSize(new Dimension(9999,22));
            setPreferredSize(new Dimension(120,22));
            
            setText(friendConnection.getConfiguration().getUserInputLocalID());
            setIcon(friendConnection.getConfiguration().getIcon());
        } else {
            setVisible(false);
        }
    }
}