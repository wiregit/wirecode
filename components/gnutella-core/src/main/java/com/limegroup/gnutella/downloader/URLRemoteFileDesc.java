package com.limegroup.gnutella.downloader;


import java.net.URL;
import java.util.Set;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.SpeedConstants;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.RemoteHostMemento;

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
	public URLRemoteFileDesc(String host, int port, String filename,
                             long size, Set<? extends URN> urns,
                             URL url) {
        super(host, port, 1, filename, size, new byte[16], SpeedConstants.T3_SPEED_INT, false,
              3, false, null, urns, false, false, "", null, -1, 0, false);
        this._url=url;
    }

    /** 
     * Returns the URL specified at construction time, which might be totally
     * independent of getName()/getIndex().  
     */
    public URL getUrl() {
        return _url;
    }
    
    @Override
    public RemoteHostMemento toMemento() {
        RemoteHostMemento memento = super.toMemento();
        memento.setCustomUrl(_url);
        return memento;
    }
}
