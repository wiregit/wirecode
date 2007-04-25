package com.limegroup.gnutella.uploader;

import java.io.IOException;
import java.util.Locale;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.statistics.UploadStat;
import com.limegroup.gnutella.util.URLDecoder;

class FileRequestParser {

    enum RequestType { FILE, THEX }; 
    
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
        int slash1Index = requestLine.indexOf("/");
        int slash2Index = requestLine.indexOf("/", slash1Index + 1);
        if ((slash1Index == -1) || (slash2Index == -1)) {
            return false;
        }
        String idString = requestLine.substring(slash1Index + 1, slash2Index);
        return idString.equalsIgnoreCase("uri-res");
        // return requestLine.startsWith("/uri-res/");
    }

    /**
     * Performs the parsing for a traditional HTTP Gnutella get request,
     * returning a new <tt>RequestLine</tt> instance with the data for the
     * request.
     * 
     * @param requestLine the HTTP get request string
     * @return a new <tt>FileRequest</tt> instance for the request or
     *         <code>null</code> if the request is malformed
     */
    public static FileRequest parseTraditionalGet(final String requestLine)
            throws IOException {
        try {
            int index = -1;
    
            // file information part: /get/0/sample.txt
            String fileName = null;
    
            int g = requestLine.indexOf("/get/");
    
            // find the next "/" after the "/get/", the number in between is the
            // index
            int d = requestLine.indexOf("/", (g + 5));
    
            // get the index
            String str_index = requestLine.substring((g + 5), d);
            index = java.lang.Integer.parseInt(str_index);
            // get the filename, which should be right after
            // the "/", and before the next " ".
            try {
                fileName = URLDecoder.decode(requestLine.substring(d + 1));
            } catch (IllegalArgumentException e) {
                fileName = requestLine.substring(d + 1);
            }
            UploadStat.TRADITIONAL_GET.incrementStat();
    
            return new FileRequest(index, fileName);
        } catch (NumberFormatException e) {
            throw new IOException();
        } catch (IndexOutOfBoundsException e) {
            throw new IOException();
        }
    }

    /**
     * Parses the get line for a URN request, throwing an exception if there are
     * any errors in parsing.
     * 
     * If we do not have the URN, we request a HttpRequestLine whose index is
     * BAD_URN_QUERY_INDEX. It is up to HTTPUploader to properly read the index
     * and set the state to FILE_NOT_FOUND.
     * 
     * @param uri the <tt>String</tt> instance containing the get
     *        request
     * @return a new <tt>RequestLine</tt> instance containing all of the data
     *         for the get request
     * @throws IOException
     */
    public static FileRequest parseURNGet(final String uri)
            throws IOException {
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
    
        FileDesc desc = RouterService.getFileManager().getFileDescForUrn(urn);
        if (desc == null) {
            UploadStat.UNKNOWN_URN_GET.incrementStat();
            return null;
        }
    
        UploadStat.URN_GET.incrementStat();
        return new FileRequest(desc.getIndex(), desc.getFileName(), requestType);
    }

    static class FileRequest {

        String filename;
        
        int index;
    
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
