
package com.limegroup.gnutella.uploader;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPMessage;
import com.limegroup.gnutella.http.HTTPUtils;

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
	
	protected void writeAlts(Writer os) throws IOException {
		if(FILE_DESC != null) {
			// write the URN in case the caller wants it
			URN sha1 = FILE_DESC.getSHA1Urn();
			if(sha1 != null) {
				HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN,
									  sha1,
									  os);
                Set<? extends AlternateLocation> alts = UPLOADER.getNextSetOfAltsToSend();
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
	
	protected void writeAlts(OutputStream os) throws IOException {
		if(FILE_DESC != null) {
			// write the URN in case the caller wants it
			URN sha1 = FILE_DESC.getSHA1Urn();
			if(sha1 != null) {
				HTTPUtils.writeHeader(HTTPHeaderName.GNUTELLA_CONTENT_URN,
									  sha1,
									  os);
                Set<? extends AlternateLocation> alts = UPLOADER.getNextSetOfAltsToSend();
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
	
	protected void writeRanges(Writer os) throws IOException {
		if (FILE_DESC !=null && FILE_DESC instanceof IncompleteFileDesc){
			URN sha1 = FILE_DESC.getSHA1Urn();
			if (sha1!=null) {
				IncompleteFileDesc iFILE_DESC = (IncompleteFileDesc)FILE_DESC;
				HTTPUtils.writeHeader(HTTPHeaderName.AVAILABLE_RANGES,
                                  iFILE_DESC, os);
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
	protected void writeProxies(Writer os) throws IOException {
	    StringBuilder buf = getProxiesBuffer();        
        if(buf.length() > 0)
            HTTPUtils.writeHeader(HTTPHeaderName.PROXIES,buf.toString(),os);
    }
    
    /**
     * writes out the X-Push-Proxies header as specified by 
     * section 4.2 of the Push Proxy proposal, v. 0.7
     */
    protected void writeProxies(OutputStream os) throws IOException {
        StringBuilder buf = getProxiesBuffer();        
        if(buf.length() > 0)
            HTTPUtils.writeHeader(HTTPHeaderName.PROXIES,buf.toString(),os);
    }
    
    /**
     * Gets the the X-Push-Proxies header as specified by 
     * section 4.2 of the Push Proxy proposal, v. 0.7
     */
    private StringBuilder getProxiesBuffer() {
        if (RouterService.acceptedIncomingConnection())
            return new StringBuilder();
        
        
        Set<? extends Connectable> proxies = RouterService.getConnectionManager().getPushProxies();
        StringBuilder buf = new StringBuilder();
	    int proxiesWritten = 0;
        BitNumbers bn = new BitNumbers(proxies.size());
        for(Connectable current : proxies) {
            if(proxiesWritten >= 4)
                break;
            
            if(current.isTLSCapable())
                bn.set(proxiesWritten);
	        buf.append(current.getAddress())
	        	.append(":")
	        	.append(current.getPort())
	        	.append(",");
	        
	        proxiesWritten++;
	    }
        
        if(!bn.isEmpty())
            buf.insert(0, PushEndpoint.PPTLS_HTTP + "=" + bn.toHexString() + ";");
	    
	    if (proxiesWritten > 0)
	        buf.deleteCharAt(buf.length()-1);
        
        return buf;	    
	}
}
