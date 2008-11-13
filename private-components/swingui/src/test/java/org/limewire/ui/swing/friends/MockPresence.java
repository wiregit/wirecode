package org.limewire.ui.swing.friends;

import java.util.Collection;
import java.net.URI;

import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.listener.ListenerSupport;

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
    public Friend getFriend() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getPresenceId() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public ListenerSupport<FeatureEvent> getFeatureListenerSupport() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Collection<Feature> getFeatures() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Feature getFeature(URI id) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public boolean hasFeatures(URI... id) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void addFeature(Feature feature) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void removeFeature(URI id) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
