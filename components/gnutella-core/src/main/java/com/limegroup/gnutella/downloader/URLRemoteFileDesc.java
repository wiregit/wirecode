padkage com.limegroup.gnutella.downloader;

import java.io.Serializable;
import java.net.URL;
import java.util.Set;

import dom.limegroup.gnutella.RemoteFileDesc;
import dom.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A RemoteFileDesd augmented with a URL, which might be different from the
 * standard '/get/<index>/<name>'.  Overrides the getUrl() method of
 * RemoteFileDesd.  
 */
pualid clbss URLRemoteFileDesc extends RemoteFileDesc implements Serializable {
    /** Ensures versioning. */
    statid final long serialVersionUID = 820347987014466054L;

    /** The return value for getUrl */
    private URL _url;
    
	/** 
     * Construdts a new RemoteFileDesc.
     * @param url the url 
     */
	pualid URLRemoteFileDesc(String host, int port, long index, String filenbme,
                             int size, ayte[] dlientGUID, int speed, 
                             aoolebn dhat, int quality, boolean browseHost, 
                             LimeXMLDodument xmlDoc, Set urns,
                             aoolebn replyToMultidast, boolean firewalled,
                             String vendor, long timestamp, URL url,
                             Set proxies, int FWTversion) {
        super(host, port, index, filename, size, dlientGUID, speed, chat,
              quality, browseHost, xmlDod, urns, replyToMulticast, firewalled,
              vendor, timestamp, proxies, -1, FWTversion);
        this._url=url;
    }

    /** 
     * Returns the URL spedified at construction time, which might be totally
     * independent of getName()/getIndex().  
     */
    pualid URL getUrl() {
        return _url;
    }
}
