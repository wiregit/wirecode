package com.limegroup.gnutella.downloader.serial.conversion;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.limewire.util.GenericsUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;

class SerialRemoteFileDesc implements Serializable {

    private static final long serialVersionUID = 6619479308616716538L;

    private String _host;

    private int _port;

    private String _filename;

    private long _index;

    private byte[] _clientGUID;

    private int _speed;

    private int _size;

    private boolean _chatEnabled;

    private int _quality;

    private boolean _replyToMulticast;

    private SerialXml[] _xmlDocs;

    private Set<URN> _urns;

    private boolean _browseHostEnabled;

    private boolean _firewalled;

    private String _vendor;

    private boolean _http11;

    private Map<String, Serializable> propertiesMap;
    
    private transient long longSize;
    private transient boolean tlsCapable;
    private transient String httpPushAddr;

    private void writeObject(ObjectOutputStream output) throws IOException {}
    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        if (_urns != null) {
            Set<URN> newUrns = GenericsUtils.scanForSet(_urns, URN.class,
                    GenericsUtils.ScanMode.NEW_COPY_REMOVED, UrnSet.class);
            if (_urns != newUrns)
                _urns = Collections.unmodifiableSet(newUrns);
        }
        
        // if we saved any properties, read them now
        if (propertiesMap != null) {
            Boolean tlsBoolean = (Boolean)propertiesMap.get("CONNECT_TYPE");
            if(tlsBoolean != null)
                tlsCapable = tlsBoolean.booleanValue();
            
            String http = (String)propertiesMap.get("PUSH_ADDR");
            // try the older serialized name if it didn't have the newer one.
            if(http == null)
                http = (String)propertiesMap.get("_pushAddr");
            
            Long size64 = (Long)propertiesMap.get("LONG_SIZE");
            if (size64 == null)
                longSize = _size;
            else
                longSize = size64.longValue();
        } else {
            // very old format, make sure we get the size right
            longSize = _size;
        }
    }
    
    public boolean isTlsCapable() {
        return tlsCapable;
    }
    
    public String getHttpPushAddr() {
        return httpPushAddr;
    }

    public String getHost() {
        return _host;
    }

    public int getPort() {
        return _port;
    }

    public String getFilename() {
        return _filename;
    }

    public long getIndex() {
        return _index;
    }

    public byte[] getClientGUID() {
        return _clientGUID;
    }

    public int getSpeed() {
        return _speed;
    }

    public long getSize() {
        return longSize;
    }

    public boolean isChatEnabled() {
        return _chatEnabled;
    }

    public int getQuality() {
        return _quality;
    }

    public boolean isReplyToMulticast() {
        return _replyToMulticast;
    }

    public String getXml() {
        return _xmlDocs != null && _xmlDocs.length > 0 ? _xmlDocs[0].getXml() : null;
    }

    public Set<URN> getUrns() {
        return _urns;
    }

    public boolean isBrowseHostEnabled() {
        return _browseHostEnabled;
    }

    public boolean isFirewalled() {
        return _firewalled;
    }

    public String getVendor() {
        return _vendor;
    }

    public boolean isHttp11() {
        return _http11;
    }

    public Map<String, Serializable> getPropertiesMap() {
        return propertiesMap;
    }
    
    

}
