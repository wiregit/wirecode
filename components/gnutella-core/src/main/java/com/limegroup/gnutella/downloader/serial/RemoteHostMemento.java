package com.limegroup.gnutella.downloader.serial;

import java.io.Serializable;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.limegroup.gnutella.URN;

/** A memento for a remote host. */
public class RemoteHostMemento implements Serializable {
    
    private static final long serialVersionUID = 1452696797555431199L;

    private static enum Keys {
        HOST, PORT, FILENAME, INDEX, CLIENTGUID,
        SPEED, SIZE, CHAT, QUALITY, REPLY_TO_MULTICAST,
        XML, URNS, BH, FIREWALLED, VENDOR, HTTP11,
        TLS, PUSH_ADDR, CUSTOM_URL
    }
    
    private final Map<Keys, Serializable> propertiesMap;
        
    public RemoteHostMemento(String host, int port, String filename, long index, byte[] clientGuid,
            int speed, long size, boolean chat, int quality, boolean replyToMulticast, String xml,
            Set<URN> urns, boolean browseHost, boolean firewalled, String vendor, boolean http1,
            boolean tls, String pushAddr) {

        this.propertiesMap = new HashMap<Keys, Serializable>(Keys.values().length);

        propertiesMap.put(Keys.HOST, host);
        propertiesMap.put(Keys.PORT, port);
        propertiesMap.put(Keys.FILENAME, filename);
        propertiesMap.put(Keys.INDEX, index);
        propertiesMap.put(Keys.CLIENTGUID, clientGuid);
        propertiesMap.put(Keys.SPEED, speed);
        propertiesMap.put(Keys.SIZE, size);
        propertiesMap.put(Keys.CHAT, chat);
        propertiesMap.put(Keys.QUALITY, quality);
        propertiesMap.put(Keys.REPLY_TO_MULTICAST, replyToMulticast);
        propertiesMap.put(Keys.XML, xml);
        propertiesMap.put(Keys.URNS, (Serializable) urns);
        propertiesMap.put(Keys.BH, browseHost);
        propertiesMap.put(Keys.FIREWALLED, firewalled);
        propertiesMap.put(Keys.VENDOR, vendor);
        propertiesMap.put(Keys.HTTP11, http1);
        propertiesMap.put(Keys.TLS, tls);
        propertiesMap.put(Keys.PUSH_ADDR, pushAddr);
    }
    
    public String getHost() { return (String)propertiesMap.get(Keys.HOST); }    
    public int getPort() { return (Integer)propertiesMap.get(Keys.PORT); }
    public String getFileName() { return (String)propertiesMap.get(Keys.FILENAME); }
    public long getIndex() { return (Long)propertiesMap.get(Keys.INDEX); }
    public byte[] getClientGuid() { return (byte[])propertiesMap.get(Keys.CLIENTGUID); }
    public int getSpeed() { return (Integer)propertiesMap.get(Keys.SPEED); }
    public long getSize() { return (Long)propertiesMap.get(Keys.SIZE); }
    public boolean isChat() { return (Boolean)propertiesMap.get(Keys.CHAT); }
    public int getQuality() { return (Integer)propertiesMap.get(Keys.QUALITY); }
    public boolean isReplyToMulticast() { return (Boolean)propertiesMap.get(Keys.REPLY_TO_MULTICAST); }
    public String getXml() { return (String)propertiesMap.get(Keys.XML); }
    @SuppressWarnings("unchecked")
    public Set<URN> getUrns() { return (Set<URN>)propertiesMap.get(Keys.URNS); }
    public boolean isBrowseHost() { return (Boolean)propertiesMap.get(Keys.BH); }
    public boolean isFirewalled() { return (Boolean)propertiesMap.get(Keys.FIREWALLED); }
    public String getVendor() { return (String)propertiesMap.get(Keys.VENDOR); }
    public boolean isHttp11() { return (Boolean)propertiesMap.get(Keys.HTTP11); }
    public boolean isTls() { return (Boolean)propertiesMap.get(Keys.TLS); }
    public String getPushAddr() { return (String)propertiesMap.get(Keys.PUSH_ADDR); }
    public URL getCustomUrl() { return (URL)propertiesMap.get(Keys.CUSTOM_URL); }
    
    public void setCustomUrl(URL url) {
        propertiesMap.put(Keys.CUSTOM_URL, url);
    }
    
}
