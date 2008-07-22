package org.limewire.xmpp.client.service;

import org.limewire.io.IpPort;

import java.util.Set;
import java.net.UnknownHostException;

public interface HostMetaData {
    public String getIP();
    public int getPort();
    public byte [] getClientID();
    public int getSpeed();
    public boolean isChatSupported();
    public int getQualityOfService();
    public boolean isBrowseHostSupported();
    public boolean isMultiCastReply();
    public boolean isFirewalled();
    public String getVendor();
    public Set<? extends IpPort> getPushProxies() throws UnknownHostException;
    public int getFirewallTransferVersion();
    public boolean isTLSCapable();
    public String toXML();
}
