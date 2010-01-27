package org.limewire.ui.swing.home;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.jdesktop.swingx.JXPanel;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationModuleEvent;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.inject.LazySingleton;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.BrowserUtils;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.components.HTMLPane;
import org.limewire.ui.swing.components.HTMLPane.LoadResult;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

import com.google.inject.Inject;

/** The main home page.*/
@LazySingleton
public class HomePanel extends JXPanel {
    
    private static final String DEFAULT_URL = "http://client-data.limewire.com/client_startup/home/";
    
    private final Application application;
    private final Browser browser;
    private final HTMLPane fallbackBrowser;
    private final GnutellaConnectionManager gnutellaConnectionManager;
    
    private boolean loadedOnce = false;
    private int retryCount = 0;
    private boolean firstRequest = true;
    private long initialLoadTime = -1;
    private boolean isProLoadState = false;

    @Inject
    public HomePanel(Application application, final Navigator navigator, GnutellaConnectionManager gnutellaConnectionManager) {
        this.application = application;
        this.gnutellaConnectionManager = gnutellaConnectionManager;
        setPreferredSize(new Dimension(500, 500));
        
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        
        if(MozillaInitialization.isInitialized()) {            
            // Hide the page when the browser goes away.
            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentHidden(ComponentEvent e) {
                    browser.load("about:blank");
                }
            });
            
            BrowserUtils.addTargetedUrlAction("_lwHome", new UriAction() {
                @Override
                public boolean uriClicked(final TargetedUri targetedUrl) {
                    SwingUtils.invokeNowOrLater(new Runnable() {
                        @Override
                        public void run() {
                            navigator.getNavItem(NavCategory.LIMEWIRE, HomeMediator.NAME).select();
                            load(targetedUrl.getUri());
                        }
                    });
                    return true;
                }
            });
            
            browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT) {
                @Override
                public void pageLoadStopped(final boolean failed) {
                    super.pageLoadStopped(failed);
                    SwingUtils.invokeNowOrLater(new Runnable() {
                        @Override
                        public void run() {
                            pageLoadFinished(!failed);
                        }
                    });
                }
            };
            fallbackBrowser = null;
            add(browser, gbc);
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
            JScrollPane scroller = new JScrollPane(fallbackBrowser,
                    JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroller.setBorder(BorderFactory.createEmptyBorder());
            add(scroller, gbc);
        }
    }
    
    @Inject void register(final ActivationManager activationManager) {        
        isProLoadState = activationManager.isProActive();
        
        gnutellaConnectionManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(GnutellaConnectionManager.CONNECTION_STRENGTH.equals(evt.getPropertyName())) {
                    reloadDefaultUrlIfPossibleAndNeeded();
                }
            }
        });

        activationManager.addModuleListener(new EventListener<ActivationModuleEvent>(){
            @Override
            public void handleEvent(ActivationModuleEvent event) {
                // we need to check on isProActive rather than the event.getData()
                // since isProActive will always return the correct value while the event.getData()
                // may be old modules being removed
                handleProStateChange(activationManager.isProActive());
            }
        });
    }
    
    private void handleProStateChange(boolean currentState) {
        // if the homepanel is visible and pro was enabled or disabled,
        // try reloading the homepage
        if(HomePanel.this.isVisible() && currentState != isProLoadState) {
            isProLoadState = currentState;
            loadDefaultUrl();
        }
    }
    
    private boolean isRequestInProgress() {
        if(MozillaInitialization.isInitialized()) {
            return browser.isRequestInProgress();
        } else {
            return fallbackBrowser.isRequestInProgress();
        }
    }
    
    /** Notification that a page finished loading. */
    private void pageLoadFinished(boolean success) {
        if(!success) {
            reloadDefaultUrlIfPossibleAndNeeded();
        }
    }
    
    /**
     * Reloads the default URL, with some extra parameters, if we need to.
     */
    private void reloadDefaultUrlIfPossibleAndNeeded() {
        // based on:
        // * if the first request was already sent succesfully
        // * if there's no active request already
        // * if the retryCount is below 5 (we don't want to hammer)
        // * if we tried to load atleast once 
        // * if the last request was successfull
        // * if our current strength indicates we're online
        if (firstRequest && !isRequestInProgress()
                && retryCount < 5 && loadedOnce && !isLastRequestSuccessful()
                && gnutellaConnectionManager.getConnectionStrength().isOnline()) {
            retryCount++;
            long delay = System.currentTimeMillis() - initialLoadTime;
            load(DEFAULT_URL + "?rd=" + delay + "&rc=" + retryCount);
        }
    }
    
    /** Returns true if the last request was succesful. */
    private boolean isLastRequestSuccessful() {
        if(MozillaInitialization.isInitialized()) {
            return browser.isLastRequestSuccessful();
        } else {
            return fallbackBrowser.isLastRequestSuccessful();
        }
    }
    
    public void loadDefaultUrl() {
        load(DEFAULT_URL);
    }

    public void load(String url) {
        loadedOnce = true;
        
        url = application.addClientInfoToUrl(url);
        
        if(firstRequest) {
            if(isLastRequestSuccessful()) {
                firstRequest = false;
            } else {
                url += "&firstRequest=true";
                if(initialLoadTime == -1) {
                    initialLoadTime = System.currentTimeMillis();
                }
            }
        }
        
        if(MozillaInitialization.isInitialized()) {
            // Reset the page to blank before continuing -- blocking is OK because this is fast.
            MozillaAutomation.blockingLoad(browser, "about:blank");
            browser.load(url);
        } else {       
            url += "&html32=true";
            
            URL bgImage = HomePanel.class.getResource("/org/limewire/ui/swing/mainframe/resources/icons/static_pages/body_bg.png");
            URL topImage = HomePanel.class.getResource("/org/limewire/ui/swing/mainframe/resources/icons/static_pages/header_logo.png");                    
            String offlinePage = "<html><head><style type=\"text/css\">* {margin: 0;  padding: 0;} body {background: #EAEAEA url(\""+ bgImage.toExternalForm() + "\") repeat-x left top; font-family: Arial, sans-serif;}table#layout tr td#header {  background: url(\"" + topImage.toExternalForm() + "\") no-repeat center top;}table#layout tr td h2 {  font-size: 16px;  margin: 0 0 8px 0;  color: #807E7E;}table#layout tr td p {  font-size: 11px;  color: #931F22;}</style></head><body><center>  <table id=\"layout\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" width=\"400\" style=\"margin: 46px 0 0 0\">    <tr valign=\"top\">      <td id=\"header\" height=\"127\" align=\"center\"></td>    </tr>    <tr valign=\"top\">      <td align=\"center\">        <h2>You are offline</h2>        <p>Please check your internet connection.</p>      </td>    </tr>  </table></center></body></html>";
            
            fallbackBrowser.setPageAsynchronous(url, offlinePage).addFutureListener(new EventListener<FutureEvent<LoadResult>>() {
                @SwingEDTEvent
                public void handleEvent(FutureEvent<LoadResult> event) {
                    pageLoadFinished(event.getResult() == LoadResult.SERVER_PAGE);
                }
            });
        }
    }    
}
