
package com.limegroup.gnutella.uploader;

import java.io.*;
import com.limegroup.gnutella.*;

import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPMessage;
import com.limegroup.gnutella.http.HTTPUtils;
import java.util.*;

/**
 * an Upload State.  has some utility methods all upload states can use.
 */
public abstract class UploadState implements HTTPMessage {

	protected final HTTPUploader UPLOADER;
	protected final FileDesc FILE_DESC;
	
	public UploadState() {
		this(null);
	}
	
	public UploadState(HTTPUploader uploader) {
		UPLOADER=uploader;
		if (uploader!=null)
			FILE_DESC=uploader.getFileDesc();
		else
			FILE_DESC=null;
	}
	
	protected void writeAlts(OutputStream os) throws IOException {
		if(FILE_DESC != null) {
			// write the URN in case the caller wants it
			URN sha1 = FILE_DESC.getSHA1Urn();
			if(sha1 != null) {
				HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN,
									  sha1,
									  os);
                Set alts = UPLOADER.getNextSetOfAltsToSend();
				if(alts.size() > 0) {
					HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                          new HTTPHeaderValueCollection(alts),
                                          os);
				}
				
				if (UPLOADER.wantsFAlts()) {
					alts = UPLOADER.getNextSetOfPushAltsToSend();
					if (alts.size()>0)
						HTTPUtils.writeHeader(HTTPHeaderName.FALT_LOCATION,
	                                     new HTTPHeaderValueCollection(alts),
	                                     os);
					
					
				}
				
			}

		}
	}
	
	protected void writeRanges(OutputStream os) throws IOException {
		if (FILE_DESC !=null && FILE_DESC instanceof IncompleteFileDesc){
			URN sha1 = FILE_DESC.getSHA1Urn();
			if (sha1!=null) {
				IncompleteFileDesc iFILE_DESC = (IncompleteFileDesc)FILE_DESC;
				HTTPUtils.writeHeader(HTTPHeaderName.AVAILABLE_RANGES,
                                  iFILE_DESC, os);
			}
		}
	}
	
	
	/**
	 * writes out the X-Push-Proxies header as specified by 
	 * section 4.2 of the Push Proxy proposal, v. 0.7
	 */
	protected void writeProxies(OutputStream os) throws IOException {
	    
	    if (RouterService.acceptedIncomingConnection())
	        return;
	    
	    
	    Set proxies = RouterService.getConnectionManager().getPushProxies();
	    
	    StringBuffer buf = new StringBuffer();
	    int proxiesWritten =0;
	    for (Iterator iter = proxies.iterator();
	    	iter.hasNext() && proxiesWritten <4 ;) {
	        PushProxyInterface current = (PushProxyInterface)iter.next();
	        buf.append(current.getPushProxyAddress().getHostAddress())
	        	.append(":")
	        	.append(current.getPushProxyPort())
	        	.append(",");
	        
	        proxiesWritten++;
	    }
	    
	    if (proxiesWritten >0)
	        buf.deleteCharAt(buf.length()-1);
	    else
	        return;
	    
	    HTTPUtils.writeHeader(HTTPHeaderName.PROXIES,buf.toString(),os);
	    
	}

}
