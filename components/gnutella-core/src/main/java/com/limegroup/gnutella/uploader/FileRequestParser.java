package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.util.Locale;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.util.URLDecoder;

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
     * Parses a Gnutella GET request.
     * 
     * @param uri the requested URI
     * @return information about the requested file
     * @throws IOException if the request is malformed
     */
    public static FileRequest parseTraditionalGet(final String uri)
            throws IOException {
        try {
            int index = -1;
    
            // file information part: /get/0/sample.txt
            String fileName = null;
    
            int g = uri.indexOf("/get/");
    
            // find the next "/" after the "/get/", the number in between is the
            // index
            int d = uri.indexOf("/", (g + 5));
    
            // get the index
            String str_index = uri.substring((g + 5), d);
            index = java.lang.Integer.parseInt(str_index);
            // get the filename, which should be right after
            // the "/", and before the next " ".
            try {
                fileName = URLDecoder.decode(uri.substring(d + 1));
            } catch (IllegalArgumentException e) {
                fileName = uri.substring(d + 1);
            }
    
            return new FileRequest(index, fileName);
        } catch (NumberFormatException e) {
            throw new IOException();
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
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
    
        FileDesc desc = fileManager.getSharedFileDescForUrn(urn);
        if (desc == null) {
            return null;
        }
    
        return new FileRequest(desc.getIndex(), desc.getFileName(), requestType);
    }

    /** Record for storing information about a file request. */
    static class FileRequest {

        /** Requested filename. */
        String filename;
        
        /** Requested index. */
        int index;
    
        /** Type of the requested resource. */ 
        RequestType requestType;
    
        public FileRequest(int index, String filename, RequestType requestType) {
            this.index = index;
            this.filename = filename;
            this.requestType = requestType;
        }
    
        public FileRequest(int index, String filename) {
            this(index, filename, RequestType.FILE);
        }
    
        public boolean isThexRequest() {
            return this.requestType == RequestType.THEX;
        }
        
        @Override
        public String toString() {
            return getClass().getName() + " [index=" + index + ",filename=" + filename + ",type=" + requestType + "]";
        }
        
    }

    
}
