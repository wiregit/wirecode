package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class LeftPanel extends JPanel {
    
    
    public LeftPanel() {
        add(new JLabel("left"));
        setBackground(Color.YELLOW);
        setMinimumSize(new Dimension(150, 0));
        setMaximumSize(new Dimension(150, Integer.MAX_VALUE));
        setPreferredSize(new Dimension(150, 700));
    }

}
