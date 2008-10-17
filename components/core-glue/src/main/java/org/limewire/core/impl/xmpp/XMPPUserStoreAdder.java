package org.limewire.core.impl.xmpp;

import java.io.UnsupportedEncodingException;

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

@Singleton
public class XMPPUserStoreAdder implements RegisteringEventListener<RosterEvent> {

    private static final Log LOG = LogFactory.getLog(XMPPUserStoreAdder.class);

    private final UserStore userStore;
    private final MACCalculator passwordCreator;    

    @Inject
    XMPPUserStoreAdder(UserStore userStore, MACCalculator macCalculator) {
        this.userStore = userStore;
        this.passwordCreator = macCalculator;
    }

    @Inject
    public void register(ListenerSupport<RosterEvent> rosterEventListenerSupport) {
        rosterEventListenerSupport.addListener(this);
    }

    public void handleEvent(final RosterEvent event) {
        try {
            userStore.addUser(event.getSource().getId(), new String(passwordCreator.getMACBytes(new SecurityToken.TokenData() {
                public byte[] getData() {
                    return StringUtils.toUTF8Bytes(event.getSource().getId());
                }
            }), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
