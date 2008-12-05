package org.limewire.ui.swing.home;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.io.IOException;

import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;

/**
 * The main home page.
 */
public class HomePanel extends JXPanel {

    public static final String NAME = "Home";
    
    private final Application application;
    private final Browser browser;
    private final JEditorPane fallbackBrowser;

    @Inject
    public HomePanel(Application application) {
        this.application = application;
        
        setPreferredSize(new Dimension(500, 500));
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        
        if(MozillaInitialization.isInitialized()) {
            browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT);
            fallbackBrowser = null;
            add(browser, gbc);
            loadDefaultUrl();
        } else {
            browser = null;
            fallbackBrowser = new JEditorPane();
            fallbackBrowser.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        NativeLaunchUtils.openURL(e.getURL().toExternalForm());
                    }
                }
            });
            fallbackBrowser.setEditable(false);
            loadDefaultUrl();
            add(new JScrollPane(fallbackBrowser,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), gbc);
        }
    }
    
    public void loadDefaultUrl() {
        load("http://www.limewire.com/client_startup/");
    }

    public void load(String url) {
        url = application.getUniqueUrl(url);
        if(!MozillaInitialization.isInitialized()) {
            url += "&html32=true";
            try {
                fallbackBrowser.setPage(url);
            } catch(IOException iox) {
                fallbackBrowser.setContentType("text/html");
                fallbackBrowser.setText("<html><body>This is the offline home page.</body></html>");
            }
        } else {
            browser.load(url);
        }
    }    
}
