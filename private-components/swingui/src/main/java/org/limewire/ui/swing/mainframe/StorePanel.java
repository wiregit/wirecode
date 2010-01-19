package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JPanel;

import org.limewire.core.api.Application;
import org.limewire.core.settings.LWSSettings;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.HistoryEntry;
import org.limewire.ui.swing.nav.Navigator;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StorePanel extends JPanel {
    private final Browser browser;

    private final Application application;
   
    @Inject
    public StorePanel(Application application, final Navigator navigator) {
        this.application = application;
        browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT);

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(browser, gbc);
        
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                // execute store js when the store is hidden, this stops anything not 
                // needed while the store is not visible
                if(MozillaInitialization.isInitialized()) {
                    MozillaAutomation.executeJavascript(browser, "clientNavigateAway();"); 
                }
            }
            
            @Override
            public void componentShown(ComponentEvent e) {
                // execute store js when the store becomes visible again, this restarts
                // anything that may have been disabled on navigate away
                if(MozillaInitialization.isInitialized()) {
                    MozillaAutomation.executeJavascript(browser, "clientNavigateTo();"); 
                }
            }
        });     
    }
    
    public Iterable<HistoryEntry> getHistory(AtomicReference<Integer> currentPosition) {
        return browser.getHistory(10, currentPosition);
    }
    
    public void loadHistoryEntry(HistoryEntry entry) {
        browser.loadHistoryEntry(entry);
    }
    
    /**
     * Attempts to load the last page visited, if it fails
     * will load or a last page doesn't exist will load the 
     * home default page.
     */
    public void loadCurrentUrl() {
        String url = browser.getUrl();
        if(url == null || url.length() == 0) {
            loadDefaultUrl();
        }
    }
    
    public void loadDefaultUrl() {
        String url = "http://" + LWSSettings.LWS_AUTHENTICATION_HOSTNAME.get();
        if(LWSSettings.LWS_AUTHENTICATION_PORT.get() != 80) {
            url += ":" + LWSSettings.LWS_AUTHENTICATION_PORT.get();    
        }
        url += LWSSettings.LWS_AUTHENTICATION_PATH.get();
        load(url);
    }

    public void load(String url) {
        url = application.addClientInfoToUrl(url);
        // Reset the page to blank before continuing -- blocking is OK
        // because this is fast.
        MozillaAutomation.blockingLoad(browser, "about:blank");
        browser.load(url + "&isClient=true");
    }
}
