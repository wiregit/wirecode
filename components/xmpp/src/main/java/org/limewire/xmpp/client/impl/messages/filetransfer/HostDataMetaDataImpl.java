package org.limewire.xmpp.client.impl.messages.filetransfer;

import org.limewire.io.IpPort;
import org.limewire.xmpp.client.service.HostMetaData;
import org.xmlpull.v1.XmlPullParser;

import java.util.Set;

public class HostDataMetaDataImpl implements HostMetaData {
    public HostDataMetaDataImpl(XmlPullParser parser) {

    }
    public String getIP() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getPort() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public byte[] getClientID() {
        return new byte[0];  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getSpeed() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isChatSupported() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getQualityOfService() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isBrowseHostSupported() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isMultiCastReply() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isFirewalled() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getVendor() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Set<? extends IpPort> getPushProxies() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getFirewallTransferVrsion() {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isTLSCapable() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
