package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.ui.swing.friends.DisplayFriendsEvent;

import com.google.inject.Singleton;

@Singleton
class StatusPanel extends JPanel {

    public StatusPanel() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(new JLabel("status"));
        add(Box.createHorizontalGlue());
        add(new JButton(new FriendsAction("Friends")));
        setBackground(Color.GRAY);
        setMinimumSize(new Dimension(0, 20));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        setPreferredSize(new Dimension(1024, 20));
    }
    
    private class FriendsAction extends AbstractAction {
        public FriendsAction(String title) {
            super(title);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            new DisplayFriendsEvent().publish();
        }
    }
}
