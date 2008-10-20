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
     * Returns whether or not the get request for the specified line is a URN
     * request.
     * 
     * @param requestLine the <tt>String</tt> to parse to check whether it's
     *        following the URN request syntax as specified in HUGE v. 0.93
     * @return <tt>true</tt> if the request is a valid URN request,
     *         <tt>false</tt> otherwise
     */
    public static boolean isURNGet(final String requestLine) {
        // check if the string between the first pair of slashes is "uri-res"
        int slash1Index = requestLine.indexOf("/");
        int slash2Index = requestLine.indexOf("/", slash1Index + 1);
        if ((slash1Index == -1) || (slash2Index == -1)) {
            return false;
        }
        String idString = requestLine.substring(slash1Index + 1, slash2Index);
        return idString.equalsIgnoreCase("uri-res");
        
        // much simpler implementation:
        // return requestLine.startsWith("/uri-res/");
    }

    /**
     * Parses a URN request.
     * 
     * @param uri the <tt>String</tt> instance containing the get request
     * @return information about the requested file, <code>null</code> if the
     *         request type is invalid or the URN does not map to a valid file
     * @throws IOException thrown if the request is malformed
     */
    public static FileRequest parseURNGet(final FileManager fileManager, final String uri) throws IOException {
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
    
        FileDesc desc = fileManager.getFileDesc(urn);
        if(desc == null || (!fileManager.getGnutellaSharedFileList().contains(desc) 
        			&& !fileManager.getIncompleteFileList().contains(desc))) {
            return null;
        }
    
        return new FileRequest(desc, requestType);
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
