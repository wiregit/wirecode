package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.messages.*;
import com.limegroup.gnutella.altlocs.*;
import com.limegroup.gnutella.http.*;
import com.limegroup.gnutella.util.CommonUtils;
import java.util.*;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.methods.HeadMethod;
import java.net.*;
import java.io.*;

/**
 * This class handles sending HTTP HEAD requests to alternate locations,
 * propagating the download "mesh" of alternate locations for files.
 */
final class HeadRequester implements Runnable {

	/**
	 * The <tt>Set</tt> of hosts to send HEAD requests to.
	 */
	private final Set HOSTS;

	/**
	 * The <tt>URN</tt> instance to propagate through the mesh.
	 */
	private final URN RESOURCE_NAME;
	
	/**
	 * The <tt>AlternateLocationCollector</tt> to notify of any new 
	 * alternate locations that are discovered while making HEAD 
	 * requests.
	 */
	private final AlternateLocationCollector COLLECTOR;

	/**
	 * The total collection of all new alternate locations for the
	 * file.
	 */
	private final AlternateLocationCollection TOTAL_ALTS;

	/**
	 * Constructs a new <tt>HeadRequester</tt> instance for the specified
	 * <tt>List</tt> of hosts to be notified, the <tt>URN</tt> to
	 * propagate, the <tt>AlternateLocationCollector</tt> to store newly
	 * discovered locations, and the list of alternate locations to report.
	 *
	 * @param uploaders the hosts to send HEAD requests to
	 * @param resourceName the <tt>URN</tt> of the resource
	 * @param collector the <tt>AlternateLocationCollector</tt> that will
	 *  store any newly discovered locations
	 * @param totalAlts the total known alternate locations to report
	 */
	public HeadRequester(Set hosts, 
						 URN resourceName,
						 AlternateLocationCollector collector,
						 AlternateLocationCollection totalAlts) {
		HOSTS = hosts;
		RESOURCE_NAME = resourceName;
		COLLECTOR = collector;
		TOTAL_ALTS = totalAlts;
	}

	/**
	 * Implements <tt>Runnable</tt>.<p>
	 *
	 * Performs HEAD requests on the <tt>List</tt> of hosts to propagate the
	 * download mesh.
	 */
	public void run() {
        try {
            Iterator iter = HOSTS.iterator();
            while(iter.hasNext()) {
                RemoteFileDesc rfd = (RemoteFileDesc)iter.next();
                if(QueryReply.isFirewalledQuality(rfd.getQuality())) {
                    // do not attempt to make a HEAD request to firewalled hosts
                    continue;
                }
                URN urn = rfd.getSHA1Urn();
                if(urn == null) continue;
                if(!urn.equals(RESOURCE_NAME)) continue;
                
                URL url = rfd.getUrl();
                String connectTo = url.toExternalForm();
                HttpMethod head = new HeadMethod(connectTo);
                head.addRequestHeader("User-Agent",
                                      CommonUtils.getHttpServer());
                head.addRequestHeader("Cache-Control", "no-cache");
                head.addRequestHeader(
                    HTTPHeaderName.GNUTELLA_CONTENT_URN.httpStringValue(),
                    RESOURCE_NAME.httpStringValue());
                head.addRequestHeader(
                    HTTPHeaderName.ALT_LOCATION.httpStringValue(),
                    TOTAL_ALTS.httpStringValue());
                head.addRequestHeader(
                    HTTPHeaderName.CONNECTION.httpStringValue(),
                    "close");
                head.setFollowRedirects(false);
                HttpClient client = HttpClientManager.getNewClient();
                try {
                    client.executeMethod(head);
                    String contentUrn = getHeader(head,
                        HTTPHeaderName.GNUTELLA_CONTENT_URN);
                    if(contentUrn == null)
                        continue;

                    try {
                        URN reportedUrn = URN.createSHA1Urn(contentUrn); 
                        if(!reportedUrn.equals(RESOURCE_NAME)) {
                            continue;
                        }
                    } catch(IOException e) {
                        continue;
                    }
                    
                    String altLocs = getHeader(head,
                         HTTPHeaderName.ALT_LOCATION);
                    if(altLocs == null)
                        continue;

                    AlternateLocationCollection alc = 
                        AlternateLocationCollection.createCollectionFromHttpValue(altLocs);
                    if (alc == null)
                        continue;
                        
                    if(alc.getSHA1Urn().equals(COLLECTOR.getSHA1Urn()))
                        COLLECTOR.addAll(alc);

                } catch(IOException e) {
                    continue;
                } finally {
                    if(head != null)
                        head.releaseConnection();
                }
            }
        } catch(Throwable e) {
            ErrorService.error(e);
        }
	}
	
	/**
	 * Simple helper method to retrieve a header from an HttpMethod.
	 */
	private static String getHeader(HttpMethod methid, HTTPHeaderName name) {
        Header header = methid.getResponseHeader(name.httpStringValue());
        if(header == null)
            return null;
        else
            return header.getValue();
    }
}
