package org.limewire.ui.swing.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.ui.swing.browser.Browser;
import org.mozilla.browser.IMozillaWindow.VisibilityMode;

public class NewAtLimePanel extends JPanel {
    
    public NewAtLimePanel() {
        setOpaque(false);
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JLabel title = new JLabel("New @ Lime");
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        add(title, gbc);
        
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        Browser browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN);
        browser.load("http://www.limewire.com");
        browser.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        add(browser, gbc);
        
        setMinimumSize(new Dimension(Integer.MAX_VALUE, 250));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 250));
        setPreferredSize(new Dimension(Integer.MAX_VALUE, 250));
    }

}
