package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;


class TopPanel extends JPanel {
    
    public TopPanel() {
        add(new JLabel("top"));
        setBackground(Color.GREEN);
        setMinimumSize(new Dimension(0, 40));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        setPreferredSize(new Dimension(1024, 40));
    }

}
