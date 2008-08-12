package org.limewire.ui.swing.friends;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;

import com.limegroup.gnutella.gui.actions.AbstractAction;

public class TopPanel extends JPanel {
    private final IconLibrary icons;
    
    public TopPanel(IconLibrary icons) {
        this.icons = icons;
        setBackground(Color.BLACK);
        setForeground(Color.WHITE);
        setLayout(new MigLayout("", "[][][]push[]", "0[]0"));
        
        JLabel friend = new JLabel();
        add(friend);
        
        JLabel friendStatus = new JLabel();
        add(friendStatus);
        
        JXButton options = new JXButton(tr("Options"));
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
