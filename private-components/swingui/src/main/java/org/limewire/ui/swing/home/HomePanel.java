/**
 * 
 */
package org.limewire.ui.swing.home;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.JEditorPane;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.browser.Browser;
import org.mozilla.browser.MozillaInitialization;
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
        
        if(MozillaInitialization.isInitialized()) {
            Browser browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN);
            browser.load(application.getUniqueUrl("http://www.limewire.com/client_startup/"));
            add(browser, gbc);
        } else {
            JEditorPane editor = new JEditorPane();
            editor.setEditable(false);
            try {
                editor.setPage(application.getUniqueUrl("http://www.limewire.com/client_startup/?html32=true"));
            } catch(IOException iox) {
                editor.setContentType("text/html");
                editor.setText("<html><body>This is the offline home page.</body></html>");
            }
            add(editor, gbc);
        }
    }
}
