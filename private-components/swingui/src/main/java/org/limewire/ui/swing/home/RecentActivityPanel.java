package org.limewire.ui.swing.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class RecentActivityPanel extends JPanel {
    
    public RecentActivityPanel() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(new JLabel("Recent Activity"), gbc);
        
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        RecentActivityBox box = new RecentActivityBox();
        box.setPreferredSize(new Dimension(100, 100));
        box.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        add(box, gbc);
    }

}
