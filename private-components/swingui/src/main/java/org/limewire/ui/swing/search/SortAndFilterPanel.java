package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.util.FontUtils;

class SortAndFilterPanel extends JXPanel {
    
    private final JComboBox sortBox;
    private final JTextField filterBox;
    
    SortAndFilterPanel() {
        this.sortBox = new JComboBox(new String[] { "Relevance", "Size", "File Extension" } );
        this.filterBox = new JTextField();
        setBackground(new Color(100, 100, 100));
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        
        JLabel sortLabel = new JLabel("Sort by");
        FontUtils.changeFontSize(sortLabel, 1);
        FontUtils.changeStyle(sortLabel, Font.BOLD);
        sortLabel.setForeground(Color.WHITE);
        gbc.insets = new Insets(5, 10, 5, 5);
        add(sortLabel, gbc);
        gbc.insets = new Insets(5, 0, 5, 15);
        add(sortBox, gbc);
        
        JLabel filterLabel = new JLabel("Filter by");
        FontUtils.changeFontSize(filterLabel, 1);
        FontUtils.changeStyle(filterLabel, Font.BOLD);
        filterLabel.setForeground(Color.WHITE);
        gbc.insets = new Insets(5, 0, 5, 5);
        add(filterLabel, gbc);
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.ipadx = 150;
        add(filterBox, gbc);
        
        gbc.weightx = 1;
        gbc.ipady = 42;
        gbc.ipadx = 0;
        add(Box.createGlue(), gbc);
    }

}
