package org.limewire.ui.swing.friends;

import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;

public class MockPresence implements Presence {
    private String status;
    private final User user;
    private Mode mode;
    private String jid;
    
    MockPresence(User user, Mode mode, String status, String jid) {
        this.user = user;
        this.mode = mode;
        this.status = status;
        this.jid = jid;
    }

    public User getUser() {
        return user;
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getJID() {
        return jid;
    }

    @Override
    public Mode getMode() {
        return mode;
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public int getPriority() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Type getType() {
        return Type.available;
    }

    @Override
    public void setIncomingChatListener(IncomingChatListener listener) {
        // TODO Auto-generated method stub

    }
}
