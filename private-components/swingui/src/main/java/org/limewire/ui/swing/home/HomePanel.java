/**
 * 
 */
package org.limewire.ui.swing.home;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JPanel;

/**
 * The main home page.
 */
public class HomePanel extends JPanel{
    
    public static final String NAME = "Home";

    public HomePanel(){
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(500, 500));
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(0, 0, 50, 0);
        add(new HomeSearchPanel(), gbc);
        
        gbc.fill = GridBagConstraints.BOTH;
        add(new RecentActivityPanel(), gbc);
        
        
    }
}
