package org.limewire.ui.swing.friends.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.StringTokenizer;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.Network;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.LimeDomListener;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.MozillaPanel.VisibilityMode;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsICookie;
import org.mozilla.interfaces.nsICookieManager;
import org.mozilla.interfaces.nsICookieService;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsIIOService;
import org.mozilla.interfaces.nsISimpleEnumerator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIURI;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

public class FacebookLoginAction extends AbstractAction {

    private static final Log LOG = LogFactory.getLog(FacebookLoginAction.class);

    @Resource private Font goBackFont;
    @Resource private Color goBackBackground;
    
    private final FriendAccountConfiguration config;
    private final FriendConnectionFactory friendConnectionFactory;
    private final LoginPopupPanel loginPanel;
    private final Application application;

    @Inject
    public FacebookLoginAction(@Assisted FriendAccountConfiguration config,
            FriendConnectionFactory friendConnectionFactory, LoginPopupPanel loginPanel,
            Application application) {
        
        super(config.getLabel(), config.getLargeIcon());
        
        GuiUtils.assignResources(this);
        
        this.config = config;
        this.friendConnectionFactory = friendConnectionFactory;
        this.loginPanel = loginPanel;
        this.application = application;
    }
    
    @Inject 
    public void register(ListenerSupport<FriendConnectionEvent> listenerSupport) {
        listenerSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            public void handleEvent(FriendConnectionEvent event) {
                if(event.getType() == FriendConnectionEvent.Type.DISCONNECTED &&
                        event.getSource().getConfiguration().getType() == Network.Type.FACEBOOK) {
                    nsICookieManager cookieService = XPCOMUtils.getServiceProxy("@mozilla.org/cookiemanager;1",
                            nsICookieManager.class);
                    nsISimpleEnumerator enumerator = cookieService.getEnumerator();
                    while(enumerator.hasMoreElements()) {                        
                        nsICookie cookie = XPCOMUtils.proxy(enumerator.getNext(), nsICookie.class);
                        if(cookie.getHost().equals(".facebook.com")) {
                            cookieService.remove(cookie.getHost(), cookie.getName(), cookie.getPath(), false);    
                        }
                    }
                }
            }
        });
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final Browser browser = new Browser(VisibilityMode.FORCED_HIDDEN, VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT) {
            @Override
            public void onAttachBrowser(ChromeAdapter chromeAdapter,
                    ChromeAdapter parentChromeAdapter) {
                super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
                nsIDOMEventTarget eventTarget = XPCOMUtils.qi(chromeAdapter.getWebBrowser().getContentDOMWindow(),
                        nsIDOMWindow2.class).getWindowRoot();
                
                LimeDomListener limeDomListener = new LimeDomListener();
                limeDomListener.addTargetedUrlAction("", new UriAction() {
                    @Override
                    public boolean uriClicked(TargetedUri targetedUri) {
                        NativeLaunchUtils.openURL(targetedUri.getUri());
                        return true;
                    }
                });
                eventTarget.addEventListener("click", limeDomListener, true);
                
                eventTarget.addEventListener("load", new nsIDOMEventListener() {
                    @Override
                    public void handleEvent(nsIDOMEvent event) {
                        String url = getUrl();
                        if (url.contains("desktopapp.php")) {
                            nsICookieService cookieService = XPCOMUtils.getServiceProxy("@mozilla.org/cookieService;1",
                                    nsICookieService.class);
                            nsIIOService ioService = XPCOMUtils.getServiceProxy("@mozilla.org/network/io-service;1", nsIIOService.class);
                            nsIURI uri = ioService.newURI(url, null, null);
                            String cookie = cookieService.getCookieStringFromHttp(uri, null, null);
                            uri = ioService.newURI("http://facebook.com/", null, null);
                            cookie = cookieService.getCookieStringFromHttp(uri, null, null);
                            config.setAttribute("url", "http://facebook.com/");
                            config.setAttribute("cookie", cookie);
                            setUsername(config, cookie);
                            friendConnectionFactory.login(config);
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    loginPanel.finished();
                                }
                            });
                        } else if (url.contains("login")) {
                            String script = "(function() {" +
                            "    function addHiddenInput(name, value) {" +
                            "    var input = document.createElement('input'); input.type='hidden'; input.name=name; input.value=value; document.forms[0].appendChild(input);" +
                            "    }" +
                            "    addHiddenInput('persistent', '1');" +
                            "    addHiddenInput('visibility', 'true');" +
                            "})();";
                            jsexec(script);
                        }
                    }
                    @Override
                    public nsISupports queryInterface(String uuid) {
                        return null;
                    }
                }, true);
            }
        };
        
        JPanel facebookLoginPanel = new JPanel(new BorderLayout());
        facebookLoginPanel.add(browser, BorderLayout.CENTER);
        
        HyperlinkButton goBackLink = new HyperlinkButton(new AbstractAction(I18n.tr("Choose another account")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginPanel.restart();
            }
        });
        goBackLink.setFont(goBackFont);
        
        JPanel goBackPanel = new JPanel(new MigLayout("insets 1 0 2 0, gap 0, fill"));
        goBackPanel.setBackground(goBackBackground);
        goBackPanel.add(goBackLink, "align center");
        
        facebookLoginPanel.add(goBackPanel, BorderLayout.SOUTH);
        
        loginPanel.setLoginComponent(facebookLoginPanel);
        MozillaAutomation.blockingLoad(browser, "about:blank");
        // show a loading panel (but not immediately -- the blocking load finishing may hide it)
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                browser.showLoadingPanel();
            }
        });
        friendConnectionFactory.requestLoginUrl(config).addFutureListener(new EventListener<FutureEvent<String>>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FutureEvent<String> event) {
                switch (event.getType()) {
                case SUCCESS:
                    browser.load(event.getResult());
                    break;
                case EXCEPTION:
                    browser.load(application.addClientInfoToUrl("http://client-data.limewire.com/fberror/"));
                    break;
                default:
                    throw new IllegalStateException(event.getType().toString());
                }
            }
        });
    }

    private void setUsername(FriendAccountConfiguration config, String facebookCookies) {
        if(facebookCookies != null) {
            StringTokenizer allCookies = new StringTokenizer(facebookCookies, ";");
            while(allCookies.hasMoreElements()) {
                String cookieString = allCookies.nextToken().trim();
                try {
                    cookieString = URLDecoder.decode(cookieString, "UTF-8");
                    StringTokenizer cookie = new StringTokenizer(cookieString, "=");
                    if(cookie.hasMoreElements()) {
                        String cookieName = cookie.nextToken();
                        if(cookieName.equals("login_x")) {
                            if(cookie.hasMoreElements()) {
                                config.setUsername(extractEmail(cookie.nextToken()));
                                return;
                            }
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    LOG.debugf(e, "failed to decode {0}", cookieString);
                }
            }
        
    }
}

    private String extractEmail(String s) {
        int emailNameIndex = s.indexOf("\"email\"");
        s = s.substring(emailNameIndex + "\"email\"".length());
        int emailStart = s.indexOf('"');
        String email = s.substring(emailStart + 1);
        int emailEndIndex = email.indexOf('"');
        email = email.substring(0, emailEndIndex);
        return email;
    }
}