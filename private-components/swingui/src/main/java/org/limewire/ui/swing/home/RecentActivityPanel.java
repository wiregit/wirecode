package org.limewire.ui.swing.home;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
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
        JLabel title = new JLabel(tr("Recent Activity"));
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        add(title, gbc);
        
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        RecentActivityBox raBox = new RecentActivityBox();
        raBox.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        add(raBox, gbc);
        
        setMinimumSize(new Dimension(Integer.MAX_VALUE, 150));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        setPreferredSize(new Dimension(Integer.MAX_VALUE, 150));
    }

}
