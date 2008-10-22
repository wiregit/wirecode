package org.limewire.core.impl.xmpp;

import org.apache.http.auth.Credentials;
import org.limewire.http.auth.Authenticator;
import org.limewire.http.auth.AuthenticatorRegistry;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.RegisteringEventListener;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.security.MACCalculator;
import org.limewire.security.SecurityToken;
import org.limewire.security.auth.UserStore;
import org.limewire.util.StringUtils;
import org.limewire.xmpp.api.client.RosterEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class XMPPAuthenticator implements RegisteringEventListener<RosterEvent>, Authenticator {

    private static final Log LOG = LogFactory.getLog(XMPPAuthenticator.class);
    private final UserStore userStore;
    private final MACCalculator passwordCreator;    

    @Inject
    XMPPAuthenticator(UserStore userStore, @Named("xmppMACCalculator") MACCalculator macCalculator) {
        this.userStore = userStore;
        this.passwordCreator = macCalculator;
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    @Inject
    public void register(AuthenticatorRegistry registry) {
        registry.addAuthenticator(this);
    }

    public void handleEvent(final RosterEvent event) {
        userStore.addUser(event.getSource().getId(), StringUtils.getUTF8String(passwordCreator.getMACBytes(new SecurityToken.TokenData() {
            public byte[] getData() {
                return StringUtils.toUTF8Bytes(event.getSource().getId());
            }
        })));
    }

    public boolean authenticate(Credentials credentials) {
        return userStore.authenticate(credentials.getUserPrincipal().getName(), credentials.getPassword());
    }
}
