package org.limewire.xmpp.client.impl.messages;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.net.UnknownHostException;

import org.apache.commons.codec.binary.Base64;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.xmpp.client.service.HostMetaData;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class HostMetaDataImpl implements HostMetaData {
    private enum Element {
        ip, port, clientId, speed, supportsChat, qos, suportsBrowseHost, multicastReply, firewalled, vendor,
        pushProxies, firewallTransferVersion, supportsTLS
    }

    private Map<Element, String> data = new HashMap<Element, String>();

    public HostMetaDataImpl(XmlPullParser parser) throws IOException, XmlPullParserException {
        do {
            int eventType = parser.getEventType();
            if(eventType == XmlPullParser.START_TAG) {
                data.put(Element.valueOf(parser.getName()), parser.getText());
            } else if(eventType == XmlPullParser.END_TAG) {
                if(parser.getName().equals("host")) {
                    return;
                }
            }
        } while (parser.nextTag() != XmlPullParser.END_DOCUMENT);
    }
    
    public String getIP() {
        return data.get(Element.ip);
    }
    
    public void setIP(String ip) {
        data.put(Element.ip, ip);
    }

    public int getPort() {
        return Integer.valueOf(data.get(Element.port));
    }
    
    public void setPort(int port) {
        data.put(Element.port, Integer.toString(port));
    }

    public byte[] getClientID() {
        return Base64.decodeBase64(data.get(Element.clientId).getBytes());
    }
    
    public void setClientID(byte [] clientID) {
        data.put(Element.clientId, new String(Base64.encodeBase64(clientID)));
    }

    public int getSpeed() {
        return Integer.valueOf(data.get(Element.speed));
    }
    
    public void setSpeed(int speed) {
        data.put(Element.speed, Integer.toString(speed));
    }

    public boolean isChatSupported() {
        return Boolean.valueOf(data.get(Element.supportsChat));
    }
    
    public void setChatSupported(boolean chatSupported) {
        data.put(Element.supportsChat, Boolean.toString(chatSupported));
    }

    public int getQualityOfService() {
        return Integer.valueOf(data.get(Element.qos));
    }
    
    public void setQualityOfService(int qos) {
        data.put(Element.qos, Integer.toString(qos));
    }

    public boolean isBrowseHostSupported() {
        return Boolean.valueOf(data.get(Element.suportsBrowseHost));
    }
    
    public void setBrowseHostSupported(boolean browseHostSupported) {
        data.put(Element.suportsBrowseHost, Boolean.toString(browseHostSupported));
    }

    public boolean isMultiCastReply() {
        return Boolean.valueOf(data.get(Element.multicastReply));
    }
    
    public void setMulticastReply(boolean multicastReply) {
        data.put(Element.multicastReply, Boolean.toString(multicastReply));
    }

    public boolean isFirewalled() {
        return Boolean.valueOf(data.get(Element.firewalled));
    }
    
    public void setFirewalled(boolean firewalled) {
        data.put(Element.firewalled, Boolean.toString(firewalled));
    }

    public String getVendor() {
        return data.get(Element.vendor);
    }
    
    public void setVendor(String vendor) {
        data.put(Element.vendor, vendor);
    }

    public Set<? extends IpPort> getPushProxies() throws UnknownHostException {
        StringTokenizer st = new StringTokenizer(data.get(Element.pushProxies), " ");
        HashSet<IpPort> set = new HashSet<IpPort>();
        while(st.hasMoreElements()) {
            set.add(new IpPortImpl(st.nextToken()));
        }
        return set;
    }
    
    public void setPushProxies(Set<IpPort> proxies) {
        String proxiesString = "";
        for(IpPort proxy : proxies) {
            proxiesString += proxy  + " ";
        }
        data.put(Element.pushProxies, proxiesString);
    }

    public int getFirewallTransferVersion() {
        return Integer.valueOf(data.get(Element.firewallTransferVersion));
    }
    
    public void setFirewallTransferVersion(int fwtv) {
        data.put(Element.firewallTransferVersion, Integer.toString(fwtv));
    }

    public boolean isTLSCapable() {
        return Boolean.valueOf(data.get(Element.supportsTLS));
    }
    
    public void setTLSCapable(boolean tlsCapable) {
        data.put(Element.supportsTLS, Boolean.toString(tlsCapable));
    }

    public String toXML() {
        String fileMetadata = "<host>";
        for(Element element : data.keySet()) {
            fileMetadata += "<" + element.toString() + ">";
            fileMetadata += data.get(element);
            fileMetadata += "</" + element.toString() + ">";
        }
        fileMetadata += "</host>";
        return fileMetadata;
    }
}
