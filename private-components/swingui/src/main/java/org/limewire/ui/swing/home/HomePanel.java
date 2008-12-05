package org.limewire.ui.swing.home;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.components.HTMLPane;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;

/**
 * The main home page.
 */
public class HomePanel extends JXPanel {

    public static final String NAME = "Home";
    
    private boolean firstRequest = true;
    
    private final Application application;
    private final Browser browser;
    private final HTMLPane fallbackBrowser;

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
            fallbackBrowser = new HTMLPane();
            fallbackBrowser.addHyperlinkListener(new HyperlinkListener() {
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                        NativeLaunchUtils.openURL(e.getURL().toExternalForm());
                    }
                }
            });
            loadDefaultUrl();
            JScrollPane scroller = new JScrollPane(fallbackBrowser,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroller.setBorder(null);
            add(scroller, gbc);
        }
    }
    
    public void loadDefaultUrl() {
        load("http://www.limewire.com/client_startup/");
    }

    public void load(String url) {
        url = application.getUniqueUrl(url);
        if(!MozillaInitialization.isInitialized()) {
            String offlinePage = "<html><body>This is the offline home page.</body></html>";
            url += "&html32=true";
            if(firstRequest) {
                if(fallbackBrowser.isLastRequestSuccessful()) {
                    firstRequest = false;
                } else {
                    url += "&firstRequest=true";
                }
            }
            fallbackBrowser.setPageAsynchronous(url, offlinePage);
        } else {
            if(firstRequest) {
                if(browser.isLastRequestSuccessful()) {
                    firstRequest = false;
                } else {
                    url += "&firstRequest=true";
                }
            }
            browser.load(url);
        }
    }    
}
