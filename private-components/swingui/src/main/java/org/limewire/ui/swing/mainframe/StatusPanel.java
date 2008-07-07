package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

class StatusPanel extends JPanel {
    
    public StatusPanel() {
        add(new JLabel("status"));
        setBackground(Color.GRAY);
        setMinimumSize(new Dimension(0, 20));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        setPreferredSize(new Dimension(1024, 20));
    }

}
