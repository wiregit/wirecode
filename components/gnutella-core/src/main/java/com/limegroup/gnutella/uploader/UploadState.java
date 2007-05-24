
package com.limegroup.gnutella.uploader;


import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.limewire.collection.BitNumbers;
import org.limewire.io.Connectable;
import org.limewire.io.IpPort;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.PushEndpoint;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValue;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPMessage;
import com.limegroup.gnutella.http.HTTPUtils;


// TODO: this whole Writer/OutputStream duopoly sucks.

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
                Collection<DirectAltLoc> direct = UPLOADER.getNextSetOfAltsToSend();
                if(direct.size() > 0) {
                    List<HTTPHeaderValue> ordered = new ArrayList<HTTPHeaderValue>(direct.size());
                    final BitNumbers bn = new BitNumbers(direct.size());
                    for(DirectAltLoc al : direct) {
                        IpPort ipp = al.getHost();
                        if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                            bn.set(ordered.size());
                        ordered.add(al);
                    }
                    
                    // insert the tls-indexes into the collection,
                    // if any existed.
                    if(!bn.isEmpty()) {
                        ordered.add(0, new HTTPHeaderValue() {
                            public String httpStringValue() {
                                return DirectAltLoc.TLS_IDX + bn.toHexString();
                            }
                        });
                    }
                    
                    HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                          new HTTPHeaderValueCollection(ordered),
                                          os);
                }
                
                if (UPLOADER.wantsFAlts()) {
                    Set<PushAltLoc> pushes = UPLOADER.getNextSetOfPushAltsToSend();
                    if (pushes.size()>0)
                        HTTPUtils.writeHeader(HTTPHeaderName.FALT_LOCATION,
                                         new HTTPHeaderValueCollection(pushes),
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
                Collection<DirectAltLoc> direct = UPLOADER.getNextSetOfAltsToSend();
				if(direct.size() > 0) {
                    List<HTTPHeaderValue> ordered = new ArrayList<HTTPHeaderValue>(direct.size());
                    final BitNumbers bn = new BitNumbers(direct.size());
                    for(DirectAltLoc al : direct) {
                        IpPort ipp = al.getHost();
                        if(ipp instanceof Connectable && ((Connectable)ipp).isTLSCapable())
                            bn.set(ordered.size());
                        ordered.add(al);
                    }
                    
                    // insert the tls-indexes into the collection,
                    // if any existed.
                    if(!bn.isEmpty()) {
                        ordered.add(0, new HTTPHeaderValue() {
                            public String httpStringValue() {
                                return DirectAltLoc.TLS_IDX + bn.toHexString();
                            }
                        });
                    }
                    
					HTTPUtils.writeHeader(HTTPHeaderName.ALT_LOCATION,
                                          new HTTPHeaderValueCollection(ordered),
                                          os);
				}
				
				if (UPLOADER.wantsFAlts()) {
					Set<PushAltLoc> pushes = UPLOADER.getNextSetOfPushAltsToSend();
					if (pushes.size()>0)
						HTTPUtils.writeHeader(HTTPHeaderName.FALT_LOCATION,
	                                     new HTTPHeaderValueCollection(pushes),
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
            buf.insert(0, PushEndpoint.PPTLS_HTTP + "=" + bn.toHexString() + ",");
	    
	    if (proxiesWritten > 0)
	        buf.deleteCharAt(buf.length()-1);
        
        return buf;	    
	}
}
