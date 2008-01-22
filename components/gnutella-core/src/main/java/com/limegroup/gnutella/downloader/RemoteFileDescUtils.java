package com.limegroup.gnutella.downloader;

import java.io.IOException;
import java.net.URL;
import java.util.Set;

import org.apache.commons.httpclient.URI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.UrnSet;
import com.limegroup.gnutella.browser.MagnetOptions;

public class RemoteFileDescUtils {
    
    private static final Log LOG = LogFactory.getLog(RemoteFileDescUtils.class);

    /** 
     * Creates a faked-up RemoteFileDesc to pass to ManagedDownloader. File size
     * should always be passed in, if not this method will do a lookup using the URL
     * to retrieve HEAD which will result in this method blocking
     */
    @SuppressWarnings("deprecation")
    public static RemoteFileDesc createRemoteFileDesc(URL url,
        String filename, URN urn, long size) throws IOException{
        if (url==null) {
            LOG.debug("createRemoteFileDesc called with null URL");        
            return null;
        }
    
        // Use the URL class to do a little parsing for us.
    
        int port = url.getPort();
        if (port<0)
            port=80;      //assume default for HTTP (not 6346)
        
        Set<URN> urns= new UrnSet();
        if (urn!=null)
            urns.add(urn);
        
        URI uri = new URI(url);    
    
        return new URLRemoteFileDesc(
                url.getHost(),  
                port,
                filename != null ? filename : MagnetOptions.extractFileName(uri),
                size <= 0 ? HTTPUtils.contentLength(url) : size,
                urns,
                url);         //assume no firewall transfer
    }

}
