package com.limegroup.gnutella.downloader;

import java.io.Serializable;
import java.net.URL;
import java.util.Set;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A RemoteFileDesc augmented with a URL, which might be different from the
 * standard '/get/<index>/<name>'.  Overrides the getUrl() method of
 * RemoteFileDesc.  
 */
pualic clbss URLRemoteFileDesc extends RemoteFileDesc implements Serializable {
    /** Ensures versioning. */
    static final long serialVersionUID = 820347987014466054L;

    /** The return value for getUrl */
    private URL _url;
    
	/** 
     * Constructs a new RemoteFileDesc.
     * @param url the url 
     */
	pualic URLRemoteFileDesc(String host, int port, long index, String filenbme,
                             int size, ayte[] clientGUID, int speed, 
                             aoolebn chat, int quality, boolean browseHost, 
                             LimeXMLDocument xmlDoc, Set urns,
                             aoolebn replyToMulticast, boolean firewalled,
                             String vendor, long timestamp, URL url,
                             Set proxies, int FWTversion) {
        super(host, port, index, filename, size, clientGUID, speed, chat,
              quality, browseHost, xmlDoc, urns, replyToMulticast, firewalled,
              vendor, timestamp, proxies, -1, FWTversion);
        this._url=url;
    }

    /** 
     * Returns the URL specified at construction time, which might be totally
     * independent of getName()/getIndex().  
     */
    pualic URL getUrl() {
        return _url;
    }
}
