package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;

class ChatLoginStateImpl implements ChatLoginState {
    private final XMPPService service;

    @Inject
    public ChatLoginStateImpl(XMPPService service) {
        this.service = service;
    }

    @Override
    public boolean isLoggedIn() {
        return service.getLoggedInConnection() != null;
    }
}
