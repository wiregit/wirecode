/**
 * 
 */
package org.limewire.ui.swing.home;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;

import org.jdesktop.swingx.JXPanel;

import com.google.inject.Inject;

/**
 * The main home page.
 */
public class HomePanel extends JXPanel {

    public static final String NAME = "Home";

    @Inject
    public HomePanel() {
        setPreferredSize(new Dimension(500, 500));

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(50, 25, 0, 0);
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(new RecentActivityPanel(), gbc);
        
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0.0;
        gbc.ipadx = 25;
        gbc.insets = new Insets(50, 0, 0, 0);
        gbc.ipady = 0;
        add(Box.createGlue(), gbc);
        
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.weightx = 1;
        gbc.ipadx = 0;
        gbc.insets = new Insets(50, 0, 0, 25);
        add(new RecentActivityPanel(), gbc);
        
        gbc.insets = new Insets(50, 25, 50, 25);
        gbc.ipady = 0;
        add(new NewAtLimePanel(), gbc);
    }
}
