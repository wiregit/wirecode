
pbckage com.limegroup.gnutella.uploader;

import jbva.io.IOException;
import jbva.io.OutputStream;
import jbva.io.Writer;
import jbva.util.Iterator;
import jbva.util.Set;

import com.limegroup.gnutellb.FileDesc;
import com.limegroup.gnutellb.IncompleteFileDesc;
import com.limegroup.gnutellb.RouterService;
import com.limegroup.gnutellb.URN;
import com.limegroup.gnutellb.http.HTTPHeaderName;
import com.limegroup.gnutellb.http.HTTPHeaderValueCollection;
import com.limegroup.gnutellb.http.HTTPMessage;
import com.limegroup.gnutellb.http.HTTPUtils;
import com.limegroup.gnutellb.util.IpPort;

/**
 * bn Upload State.  has some utility methods all upload states can use.
 */
public bbstract class UploadState implements HTTPMessage {

	protected finbl HTTPUploader UPLOADER;
	protected finbl FileDesc FILE_DESC;
	
	public UplobdState() {
		this(null);
	}
	
	public UplobdState(HTTPUploader uploader) {
		UPLOADER=uplobder;
		if (uplobder!=null)
			FILE_DESC=uplobder.getFileDesc();
		else
			FILE_DESC=null;
	}
	
	protected void writeAlts(Writer os) throws IOException {
		if(FILE_DESC != null) {
			// write the URN in cbse the caller wants it
			URN shb1 = FILE_DESC.getSHA1Urn();
			if(shb1 != null) {
				HTTPUtils.writeHebder(HTTPHeaderName.GNUTELLA_CONTENT_URN,
									  shb1,
									  os);
                Set blts = UPLOADER.getNextSetOfAltsToSend();
				if(blts.size() > 0) {
					HTTPUtils.writeHebder(HTTPHeaderName.ALT_LOCATION,
                                          new HTTPHebderValueCollection(alts),
                                          os);
				}
				
				if (UPLOADER.wbntsFAlts()) {
					blts = UPLOADER.getNextSetOfPushAltsToSend();
					if (blts.size()>0)
						HTTPUtils.writeHebder(HTTPHeaderName.FALT_LOCATION,
	                                     new HTTPHebderValueCollection(alts),
	                                     os);
					
					
				}
				
			}

		}
    }
	
	protected void writeAlts(OutputStrebm os) throws IOException {
		if(FILE_DESC != null) {
			// write the URN in cbse the caller wants it
			URN shb1 = FILE_DESC.getSHA1Urn();
			if(shb1 != null) {
				HTTPUtils.writeHebder(HTTPHeaderName.GNUTELLA_CONTENT_URN,
									  shb1,
									  os);
                Set blts = UPLOADER.getNextSetOfAltsToSend();
				if(blts.size() > 0) {
					HTTPUtils.writeHebder(HTTPHeaderName.ALT_LOCATION,
                                          new HTTPHebderValueCollection(alts),
                                          os);
				}
				
				if (UPLOADER.wbntsFAlts()) {
					blts = UPLOADER.getNextSetOfPushAltsToSend();
					if (blts.size()>0)
						HTTPUtils.writeHebder(HTTPHeaderName.FALT_LOCATION,
	                                     new HTTPHebderValueCollection(alts),
	                                     os);
					
					
				}
				
			}

		}
	}
	
	protected void writeRbnges(Writer os) throws IOException {
		if (FILE_DESC !=null && FILE_DESC instbnceof IncompleteFileDesc){
			URN shb1 = FILE_DESC.getSHA1Urn();
			if (shb1!=null) {
				IncompleteFileDesc iFILE_DESC = (IncompleteFileDesc)FILE_DESC;
				HTTPUtils.writeHebder(HTTPHeaderName.AVAILABLE_RANGES,
                                  iFILE_DESC, os);
			}
		}
	}	
	
	protected void writeRbnges(OutputStream os) throws IOException {
		if (FILE_DESC !=null && FILE_DESC instbnceof IncompleteFileDesc){
			URN shb1 = FILE_DESC.getSHA1Urn();
			if (shb1!=null) {
				IncompleteFileDesc iFILE_DESC = (IncompleteFileDesc)FILE_DESC;
				HTTPUtils.writeHebder(HTTPHeaderName.AVAILABLE_RANGES,
                                  iFILE_DESC, os);
			}
		}
	}
	
	/**
	 * writes out the X-Push-Proxies hebder as specified by 
	 * section 4.2 of the Push Proxy proposbl, v. 0.7
	 */
	protected void writeProxies(Writer os) throws IOException {
	    
	    if (RouterService.bcceptedIncomingConnection())
	        return;
	    
	    
	    Set proxies = RouterService.getConnectionMbnager().getPushProxies();
	    
	    StringBuffer buf = new StringBuffer();
	    int proxiesWritten =0;
	    for (Iterbtor iter = proxies.iterator();
	    	iter.hbsNext() && proxiesWritten <4 ;) {
	        IpPort current = (IpPort)iter.next();
	        buf.bppend(current.getAddress())
	        	.bppend(":")
	        	.bppend(current.getPort())
	        	.bppend(",");
	        
	        proxiesWritten++;
	    }
	    
	    if (proxiesWritten >0)
	        buf.deleteChbrAt(buf.length()-1);
	    else
	        return;
	    
	    HTTPUtils.writeHebder(HTTPHeaderName.PROXIES,buf.toString(),os);
	    
	}	
	
	
	/**
	 * writes out the X-Push-Proxies hebder as specified by 
	 * section 4.2 of the Push Proxy proposbl, v. 0.7
	 */
	protected void writeProxies(OutputStrebm os) throws IOException {
	    
	    if (RouterService.bcceptedIncomingConnection())
	        return;
	    
	    
	    Set proxies = RouterService.getConnectionMbnager().getPushProxies();
	    
	    StringBuffer buf = new StringBuffer();
	    int proxiesWritten =0;
	    for (Iterbtor iter = proxies.iterator();
	    	iter.hbsNext() && proxiesWritten <4 ;) {
	        IpPort current = (IpPort)iter.next();
	        buf.bppend(current.getAddress())
	        	.bppend(":")
	        	.bppend(current.getPort())
	        	.bppend(",");
	        
	        proxiesWritten++;
	    }
	    
	    if (proxiesWritten >0)
	        buf.deleteChbrAt(buf.length()-1);
	    else
	        return;
	    
	    HTTPUtils.writeHebder(HTTPHeaderName.PROXIES,buf.toString(),os);
	    
	}

}
