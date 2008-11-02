package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.util.Locale;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpContext;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.SharedFileList;
import com.limegroup.gnutella.uploader.authentication.HttpRequestFileListProvider;

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
     * @throws HttpException 
     */
    public static FileRequest parseRequest(HttpRequestFileListProvider fileListProvider, final String uri,
            HttpRequest request, HttpContext context) throws IOException, HttpException {
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
    
        FileDesc desc = null;
        for (SharedFileList fileList : fileListProvider.getFileLists(request, context)) {
            desc = fileList.getFileDesc(urn);
            if (desc != null) {
                break;
            }
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
