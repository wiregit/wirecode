package org.limewire.ui.swing.mainframe;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;

import org.apache.http.cookie.Cookie;
import org.limewire.core.api.Application;
import org.limewire.core.api.search.store.StoreListener;
import org.limewire.core.api.search.store.StoreManager;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.store.StoreController;
import org.limewire.ui.swing.search.store.StoreControllerFactory;
import org.limewire.ui.swing.search.store.StoreDomListener;
import org.limewire.ui.swing.search.store.StoreDomListener.LoadCookieAction;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaInitialization;
import org.mozilla.browser.MozillaPanel.VisibilityMode;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StorePanel extends JPanel {
    private static final String STORE_DOMAIN = ".store.limewire.com";
    
    private final Browser browser;

    private final Application application;
    
    private final StoreController storeController;
    
    /**
     * Used to ignore the first component hidden event coming through to the
     * ComponentListener. The load and hidden events are coming out of order because
     * of the usage of card layout, and loading StorePanel lazily. When adding a component
     * to CardLayout, card layout calls setVisible false on it. The main issue is that we have
     * started loading components lazily as they are selected. So we can't force that componsnts
     * are added to the card layout before we use them.
     */
    private final AtomicBoolean firstHiddenIgnored = new AtomicBoolean(false);

    /**
     * Saves the current state of the store page. We load a blank page to
     * stop any playing media when navigating away, this saves that state
     * and reloads it when the store is brought back up.
     */
    private String currentURL;
    
    @Inject
    public StorePanel(Application application, final Navigator navigator, 
            StoreControllerFactory storeControllerFactory) {
        this.application = application;
        this.storeController = storeControllerFactory.create();
        
        browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT) {
            @Override
            public void onAttachBrowser(ChromeAdapter chromeAdapter,
                    ChromeAdapter parentChromeAdapter) {
                super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
                
                // Get DOM event target.
                nsIDOMEventTarget eventTarget = XPCOMUtils.qi(
                        chromeAdapter.getWebBrowser().getContentDOMWindow(),
                        nsIDOMWindow2.class).getWindowRoot();
                
                // Create DOM listener for load event.
                StoreDomListener storeDomListener = new StoreDomListener(this);
                storeDomListener.setLoadCookieAction(new LoginAction());
                
                eventTarget.addEventListener("load", storeDomListener, true);
            }            
        };

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        add(browser, gbc);
        
        // Hide the page when the browser goes away.
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentHidden(ComponentEvent e) {
                if (firstHiddenIgnored.getAndSet(true) && MozillaInitialization.isInitialized()) {
                    currentURL = browser.getUrl();
                    browser.load("about:blank");
                }
            }
        });     
    }
    
    /**
     * Registers a listener on the specified store manager to handle login
     * changes.
     */
    @Inject
    void register(StoreManager storeManager) {
        // Add store listener to update browser cookies.
        storeManager.addStoreListener(new StoreListener() {
            @Override
            public void loginChanged(boolean loggedIn) {
                // Get browser cookies and look for login state.
                List<Cookie> cookieList = StoreDomListener.getCookieList(STORE_DOMAIN);
                boolean loginCookieFound = isLoginCookie(cookieList);
                
                if (loggedIn) {
                    // Reload current page.  This updates the page with cookies
                    // that may have been loaded using the login dialog.
                    browser.reload();

                } else if (!loggedIn && loginCookieFound) {
                    // Remove browser cookies.
                    StoreDomListener.removeCookies(cookieList);
                    // Reload home page.
                    loadDefaultUrl();
                }
            }
        });
    }
    
    /**
     * Attempts to load the last page visited, if it fails
     * will load or a last page doesn't exist will load the 
     * home default page.
     */
    public void loadCurrentUrl() {
        if(currentURL != null) {
            browser.load(currentURL);
        } else {
            loadDefaultUrl();
        }
    }
    
    public void loadDefaultUrl() {
        load("http://store.limewire.com/");
    }

    public void load(String url) {
        url = application.addClientInfoToUrl(url);
        // Reset the page to blank before continuing -- blocking is OK
        // because this is fast.
        MozillaAutomation.blockingLoad(browser, "about:blank");
        browser.load(url + "&isClient=true");
    }
    
    /**
     * Returns true if the specified cookie list contains the login cookie.
     */
    private boolean isLoginCookie(List<Cookie> cookieList) {
        // TODO determine if login cookie is available
        return true;
    }
    
    /**
     * Action to handle cookies on successful login.
     */
    private class LoginAction implements LoadCookieAction {
        
        @Override
        public String getDomain() {
            return STORE_DOMAIN;
        }

        @Override
        public boolean isUrlValid(String url) {
            return (url.contains("store.limewire.com") && url.contains("Home"));
        }

        @Override
        public void cookiesLoaded(List<Cookie> cookieList) {
            // Search for login cookie.
            boolean loggedIn = isLoginCookie(cookieList);
            
            if (loggedIn && !storeController.isLoggedIn()) {
                // Save login cookies.
                storeController.login(cookieList);
                
            } else if (!loggedIn && storeController.isLoggedIn()) {
                // Remove cookies.
                storeController.logout();
            }
        }
    }
}