
package com.limegroup.gnutella.uploader;

import java.io.*;
import com.limegroup.gnutella.*;

import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HTTPHeaderValueCollection;
import com.limegroup.gnutella.http.HTTPMessage;
import com.limegroup.gnutella.http.HTTPUtils;
import com.sun.java.util.collections.Set;

/**
 * an Upload State.
 */
public abstract class UploadState implements HTTPMessage {

	protected final HTTPUploader UPLOADER;
	protected final FileDesc FILE_DESC;
	
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
					if(RouterService.acceptedIncomingConnection()) {
						HTTPUtils.writeHeader(HTTPHeaderName.FALT_LOCATION,
	                                          new HTTPHeaderValueCollection(alts),
	                                          os);
					}
					
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

}
