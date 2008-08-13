package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.util.FontUtils;

public class TopPanel extends JPanel {
    
    public TopPanel(IconLibrary icons) {
        setBackground(Color.BLACK);
        setForeground(Color.WHITE);
        setLayout(new MigLayout("insets 0 0 0 0", "3[][]0:push[]0[]0", "0[]0"));
        
        JLabel friend = new JLabel();
        friend.setForeground(getForeground());
        add(friend);
        
        JLabel friendStatus = new JLabel();
        friendStatus.setForeground(getForeground());
        FontUtils.changeSize(friendStatus, -1.8f);
        add(friendStatus);
        
        JXButton options = new JXButton(tr("Options"));
        FontUtils.changeSize(options, -2.8f);
        options.setForeground(getForeground());
        options.setBackground(getBackground());
        options.setBorderPainted(false);
        add(options);
        
        JXButton closeChat = new JXButton(new CloseAction(icons.getCloseChat()));
        closeChat.setBorderPainted(false);
        add(closeChat);
    }
    
    private static class CloseAction extends AbstractAction {
        public CloseAction(Icon icon) {
            super("", icon);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            new DisplayFriendsEvent().publish();
        }
    }
}
