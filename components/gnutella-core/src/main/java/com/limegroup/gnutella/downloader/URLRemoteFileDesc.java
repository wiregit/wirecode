package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import java.net.URL;
import java.io.Serializable;
import java.util.Set;

/**
 * A RemoteFileDesc augmented with a URL, which might be different from the
 * standard '/get/<index>/<name>'.  Overrides the getUrl() method of
 * RemoteFileDesc.  
 */
public class URLRemoteFileDesc extends RemoteFileDesc implements Serializable {
    /** Ensures versioning. */
    static final long serialVersionUID = 820347987014466054L;

    /** The return value for getUrl */
    private URL _url;
    
	/** 
     * Constructs a new RemoteFileDesc.
     * @param url the url 
     */
	public URLRemoteFileDesc(String host, int port, long index, String filename,
                             int size, byte[] clientGUID, int speed, 
                             boolean chat, int quality, boolean browseHost, 
                             LimeXMLDocument xmlDoc, Set urns,
                             boolean replyToMulticast, boolean firewalled,
                             String vendor, long timestamp, URL url,
                             Set proxies, boolean canDoFWTransfer) {
        super(host, port, index, filename, size, clientGUID, speed, chat,
              quality, browseHost, xmlDoc, urns, replyToMulticast, firewalled,
              vendor, timestamp, proxies, -1, canDoFWTransfer);
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
