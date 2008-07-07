/**
 * 
 */
package org.limewire.ui.swing.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JPanel;

/**
 * The main home page.
 */
public class HomePanel extends JPanel {

    public static final String NAME = "Home";
    private final HomeSearchPanel hsPanel;

    public HomePanel() {
        this.hsPanel = new HomeSearchPanel();

        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(500, 500));

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.insets = new Insets(0, 0, 100, 0);
        add(hsPanel, gbc);

        JPanel bottomPanel = new JPanel();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        bottomPanel.setMinimumSize(new Dimension(700, 200));
        bottomPanel.setPreferredSize(new Dimension(700, 200));
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new GridBagLayout());
        add(bottomPanel, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weightx = 1;
        gbc.weighty = 1;
        bottomPanel.add(new RecentActivityPanel(), gbc);
        
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        gbc.weightx = 0.0;
        gbc.ipadx = 100;
        bottomPanel.add(Box.createGlue(), gbc);

        gbc.weightx = 1;
        gbc.ipadx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        bottomPanel.add(new NewAtLimePanel(), gbc);

    }

    @Override
    public boolean requestFocusInWindow() {
        return hsPanel.requestFocusInWindow();
    }
}
