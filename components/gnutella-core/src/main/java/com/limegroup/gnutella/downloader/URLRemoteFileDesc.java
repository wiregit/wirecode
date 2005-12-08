pbckage com.limegroup.gnutella.downloader;

import jbva.io.Serializable;
import jbva.net.URL;
import jbva.util.Set;

import com.limegroup.gnutellb.RemoteFileDesc;
import com.limegroup.gnutellb.xml.LimeXMLDocument;

/**
 * A RemoteFileDesc bugmented with a URL, which might be different from the
 * stbndard '/get/<index>/<name>'.  Overrides the getUrl() method of
 * RemoteFileDesc.  
 */
public clbss URLRemoteFileDesc extends RemoteFileDesc implements Serializable {
    /** Ensures versioning. */
    stbtic final long serialVersionUID = 820347987014466054L;

    /** The return vblue for getUrl */
    privbte URL _url;
    
	/** 
     * Constructs b new RemoteFileDesc.
     * @pbram url the url 
     */
	public URLRemoteFileDesc(String host, int port, long index, String filenbme,
                             int size, byte[] clientGUID, int speed, 
                             boolebn chat, int quality, boolean browseHost, 
                             LimeXMLDocument xmlDoc, Set urns,
                             boolebn replyToMulticast, boolean firewalled,
                             String vendor, long timestbmp, URL url,
                             Set proxies, int FWTversion) {
        super(host, port, index, filenbme, size, clientGUID, speed, chat,
              qublity, browseHost, xmlDoc, urns, replyToMulticast, firewalled,
              vendor, timestbmp, proxies, -1, FWTversion);
        this._url=url;
    }

    /** 
     * Returns the URL specified bt construction time, which might be totally
     * independent of getNbme()/getIndex().  
     */
    public URL getUrl() {
        return _url;
    }
}
