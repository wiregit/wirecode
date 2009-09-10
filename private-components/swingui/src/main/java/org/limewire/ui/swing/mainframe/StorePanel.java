package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import org.limewire.core.api.Application;
import org.limewire.inject.EagerSingleton;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.BrowserUtils;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;

@EagerSingleton
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
        
        // Stop anything that may be playing in the store when the 
        // browser is hidden
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                if (MozillaInitialization.isInitialized()) {
                    stopLWSPlayer();
                }
            }
        });     
        BrowserUtils.addTargetedUrlAction("_lwStore", new UriAction() {
            @Override
            public boolean uriClicked(final TargetedUri targetedUrl) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        navigator.getNavItem(NavCategory.LIMEWIRE, StoreMediator.NAME).select();
                        load(targetedUrl.getUri());
                    }
                });
                return true;
            }
        }); 
    }
    
    /**
     * Javascript calls into the store to stop any player that may be playing.
     */
    private void stopLWSPlayer() {
        MozillaAutomation.executeJavascript(browser, "omalley.stop();"); 
        MozillaAutomation.executeJavascript(browser, "SimplePlayer.player.stop();");
        MozillaAutomation.executeJavascript(browser, "productPreviewPlayer.stop();");
    }
    
    /**
     * If a url is currently loaded, does nothing. If the first time loading this
     * browser, will load the default URL.
     */
    public void loadCurrentUrl() {
        if(browser.getUrl() == null || browser.getUrl().length() == 0) {
            loadDefaultUrl();
        }
    }
    
    public void loadDefaultUrl() {
        load("http://store.limewire.com/");
    }

    public void load(String url) {
        url = application.addClientInfoToUrl(url);
        if (!MozillaInitialization.isInitialized()) {
            NativeLaunchUtils.openURL(url);
        } else {
            // Reset the page to blank before continuing -- blocking is OK
            // because this is fast.
            MozillaAutomation.blockingLoad(browser, "about:blank");
            browser.load(url + "&isClient=true");
        }
    }
}
