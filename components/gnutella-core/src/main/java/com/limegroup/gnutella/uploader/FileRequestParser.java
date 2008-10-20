package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.util.Locale;

import org.limewire.util.StringUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;

/**
 * Provides methods for parsing Gnutella request URIs.
 */
class FileRequestParser {

    /** The type of the requested resource. */
    enum RequestType {
        /** Indicates a request for a file transfer. */ 
        FILE, 
        /** Indicates a request for a THEX tree. */ 
        THEX 
    }; 

    /**
     * Parses a URN request.
     * 
     * @param uri the <tt>String</tt> instance containing the get request
     * @return information about the requested file, <code>null</code> if the
     *         request type is invalid or the URN does not map to a valid file
     * @throws IOException thrown if the request is malformed
     */
    public static FileRequest parseRequest(final FileManager fileManager, final String uri) throws IOException {
        // Only parse URI requests.
        if(!uri.toLowerCase(Locale.US).startsWith("/uri-res/")) {
            throw new IOException("invalid request");
        }
        
        URN urn = URN.createSHA1UrnFromHttpRequest(uri + " HTTP/1.1");
    
        // Parse the service identifier, whether N2R, N2X or something
        // we cannot satisfy. URI scheme names are not case-sensitive.
        RequestType requestType;
        String requestUpper = uri.toUpperCase(Locale.US);
        if (requestUpper.indexOf(HTTPConstants.NAME_TO_THEX) > 0) {
            requestType = RequestType.THEX;
        } else if (requestUpper.indexOf(HTTPConstants.NAME_TO_RESOURCE) > 0) {
            requestType = RequestType.FILE;
        } else {
            return null;
        }
    
        FileDesc desc = fileManager.getGnutellaSharedFileList().getFileDesc(urn);
        if(desc == null) {
            desc = fileManager.getIncompleteFileList().getFileDesc(urn);
        }
        
        if(desc == null) {
            return null;
        } else {
            return new FileRequest(desc, requestType);
        }
    }

    /** Record for storing information about a file request. */
    static class FileRequest {
        
        private final FileDesc fileDesc;
    
        /** Type of the requested resource. */ 
        private final RequestType requestType;
    
        public FileRequest(FileDesc fileDesc, RequestType requestType) {
            this.fileDesc = fileDesc;
            this.requestType = requestType;
        }
    
        public boolean isThexRequest() {
            return this.requestType == RequestType.THEX;
        }
        
        public FileDesc getFileDesc() {
            return fileDesc;
        }
        
        @Override
        public String toString() {
            return StringUtils.toString(this);
        }
        
    }

    
}
