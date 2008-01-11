package com.limegroup.gnutella.downloader;


import java.net.URL;
import java.util.Set;

import org.limewire.io.IpPort;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A RemoteFileDesc augmented with a URL, which might be different from the
 * standard '/get/<index>/<name>'.  Overrides the getUrl() method of
 * RemoteFileDesc.  
 */
public class URLRemoteFileDesc extends RemoteFileDesc {

    /** The return value for getUrl */
    private URL _url;
    
	/** 
     * Constructs a new RemoteFileDesc.
     * @param url the url 
     */
	public URLRemoteFileDesc(String host, int port, long index, String filename,
                             long size, byte[] clientGUID, int speed, 
                             boolean chat, int quality, boolean browseHost, 
                             LimeXMLDocument xmlDoc, Set<? extends URN> urns,
                             boolean replyToMulticast, boolean firewalled,
                             String vendor, URL url,
                             Set<? extends IpPort> proxies, int FWTversion) {
        super(host, port, index, filename, size, clientGUID, speed, chat,
              quality, browseHost, xmlDoc, urns, replyToMulticast, firewalled,
              vendor, proxies, -1, FWTversion, false);
        this._url=url;
    }

    /** 
     * Returns the URL specified at construction time, which might be totally
     * independent of getName()/getIndex().  
     */
    public URL getUrl() {
        return _url;
    }
}
