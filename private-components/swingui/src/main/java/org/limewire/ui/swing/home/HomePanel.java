/**
 * 
 */
package org.limewire.ui.swing.home;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.browser.Browser;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;

/**
 * The main home page.
 */
public class HomePanel extends JXPanel {

    public static final String NAME = "Home";

    @Inject
    public HomePanel(Application application) {
        setPreferredSize(new Dimension(500, 500));
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        Browser browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN);
        browser.load(application.getUniqueUrl("http://www.limewire.com/client"));
        add(browser, gbc);
    }
}
