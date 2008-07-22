package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.limewire.ui.swing.browser.Browser;
import org.mozilla.browser.IMozillaWindow.VisibilityMode;

public class StorePanel  extends JPanel{
    public static final String NAME = "LimeWire Store";
    
    public StorePanel(){
        setLayout(new GridBagLayout());
        Browser browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN);
        browser.load("http://store.limewire.com");
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(browser, gbc);
    }
}
