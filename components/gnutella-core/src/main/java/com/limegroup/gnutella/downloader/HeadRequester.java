package com.limegroup.gnutella.downloader;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.http.*;
import com.sun.java.util.collections.*;
import java.net.*;
import java.io.*;

/**
 * This class handles sending HTTP HEAD requests to alternate locations,
 * propagating the download "mesh" of alternate locations for files.
 */
final class HeadRequester implements Runnable {

	/**
	 * The <tt>List</tt> of hosts to send HEAD requests to.
	 */
	private final List HOSTS;

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
	public HeadRequester(List hosts, 
						 URN resourceName,
						 AlternateLocationCollector collector,
						 AlternateLocationCollection totalAlts) {
		HOSTS = Collections.unmodifiableList(hosts);
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
                if(urn.equals(RESOURCE_NAME)) {
                    URL url = rfd.getUrl();
                    if(url == null) continue;
                    try {
                        HttpURLConnection httpConnection = 
                            (HttpURLConnection)url.openConnection();
                        httpConnection.setRequestMethod("HEAD");
                        httpConnection.setDoOutput(true);
                        httpConnection.setDoInput(true);
                        httpConnection.setUseCaches(false);
                        httpConnection.setAllowUserInteraction(false);
                        httpConnection.setRequestProperty(
                            HTTPHeaderName.CONTENT_URN.httpStringValue(), 
                            RESOURCE_NAME.httpStringValue());
                        httpConnection.setRequestProperty(
						    HTTPHeaderName.ALT_LOCATION.httpStringValue(),
						    TOTAL_ALTS.httpStringValue());
                        httpConnection.setRequestProperty(
                            HTTPHeaderName.CONNECTION.httpStringValue(),
                            "close");
                        httpConnection.connect();
                        String contentUrn = httpConnection.getHeaderField
					        (HTTPHeaderName.CONTENT_URN.httpStringValue());
                        if(contentUrn == null) {
                            continue;
                        }
                        try {
                            URN reportedUrn = URN.createSHA1Urn(contentUrn); 
                            if(!reportedUrn.equals(RESOURCE_NAME)) {
                                continue;
                            }
                        } catch(IOException e) {
                            continue;
                        }
                        String altLocs = httpConnection.getHeaderField
                            (HTTPHeaderName.ALT_LOCATION.httpStringValue());
                        if(altLocs == null) {
                            continue;
                        }
                        AlternateLocationCollection alc = 
                            AlternateLocationCollection.createCollectionFromHttpValue(altLocs);
                        COLLECTOR.addAlternateLocationCollection(alc);
                        httpConnection.disconnect();
                    } catch(IOException e) {
                        continue;
                    }			
                }
            }
        } catch(Exception e) {
            RouterService.getCallback().error(e);
        }
	}
}
