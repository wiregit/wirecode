package org.limewire.ui.swing.friends.login;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import org.limewire.concurrent.FutureEvent;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.browser.LimeDomListener;
import org.limewire.ui.swing.browser.UriAction;
import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.mozilla.browser.XPCOMUtils;
import org.mozilla.browser.impl.ChromeAdapter;
import org.mozilla.interfaces.nsICookieService;
import org.mozilla.interfaces.nsIDOMEvent;
import org.mozilla.interfaces.nsIDOMEventListener;
import org.mozilla.interfaces.nsIDOMEventTarget;
import org.mozilla.interfaces.nsIDOMWindow2;
import org.mozilla.interfaces.nsIIOService;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIURI;

class FacebookLoginAction extends AbstractAction {

    private final FriendAccountConfiguration config;
    private final FriendConnectionFactory friendConnectionFactory;
    private final LoginPopupPanel loginPanel;

    public FacebookLoginAction(FriendAccountConfiguration config,
            FriendConnectionFactory friendConnectionFactory, LoginPopupPanel loginPanel) { 
        super(config.getLabel(), config.getLargeIcon());
        this.config = config;
        this.friendConnectionFactory = friendConnectionFactory;
        this.loginPanel = loginPanel;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        final JFrame dialog = new JFrame();
        final Browser browser = new Browser() {
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
                            friendConnectionFactory.login(config);
                            dialog.setVisible(false);
                            loginPanel.finished();
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
        dialog.getContentPane().add(browser);
        dialog.pack();
        dialog.setSize(800, 600);
        dialog.setVisible(true);
        friendConnectionFactory.requestLoginUrl(config).addFutureListener(new EventListener<FutureEvent<String>>() {
            @Override
            public void handleEvent(FutureEvent<String> event) {
                switch (event.getType()) {
                case SUCCESS:
                    browser.load(event.getResult());
                    break;
                case EXCEPTION:
                    // TODO write error handling ui code 
                    throw new RuntimeException(event.getException());
                default:
                    throw new IllegalStateException(event.getType().toString());
                }
            }
        });
    }
}