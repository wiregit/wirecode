package org.limewire.ui.swing.friends;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * 
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class LoginPanel extends JPanel {

    public LoginPanel() {
        add(new JLabel("Have a Gmail or Facebook Account?"), BorderLayout.NORTH);
    }
}
