package com.limegroup.gnutella.io;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;

import org.limewire.core.settings.SharingSettings;
import org.limewire.io.URN;
import org.limewire.io.UrnSet;
import org.limewire.io.URN.Type;
import org.limewire.security.SHA1;

import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.security.MerkleTree;
import com.limegroup.gnutella.security.Tiger;

public class URNFactory {
    
    /**
     * Cached constant to avoid making unnecessary string allocations
     * in validating input.
     */
    private static final String QUESTION_MARK = "?";
    
    /**
     * Cached constant to avoid making unnecessary string allocations
     * in validating input.
     */
    private static final String SPACE = " ";
    
    /**
     * Cached constant to avoid making unnecessary string allocations
     * in validating input.
     */
    private static final String SLASH = "/";
    
    /**
     * Cached constant to avoid making unnecessary string allocations
     * in validating input.
     */
    private static final String TWO = "2";
    
   
    
    public static URN createTTRootFile(File file) throws IOException, InterruptedException {
        MessageDigest tt = new MerkleTree(new Tiger());
        return URN.generateURN(file, 0, file.length(), Type.TTROOT, tt, 
                SharingSettings.MIN_IDLE_TIME_FOR_FULL_HASHING.getValue(),
                SharingSettings.FRIENDLY_HASHING.getValue());
    }
    
    public static URN createSHA1UrnFromHttpRequest(final String requestLine) 
    throws IOException {
    if(!isValidUrnHttpRequest(requestLine)) {
        throw new IOException("INVALID URN HTTP REQUEST");
    }
    String urnString = extractUrnFromHttpRequest(requestLine);
    if(urnString == null) {
        throw new IOException("COULD NOT CONSTRUCT URN");
    }      
    return URN.createSHA1Urn(urnString);
}
    
    /**
     * Returns whether or not the http request is valid, as specified in
     * HUGE v. 0.93 and IETF RFC 2169.  This verifies everything except
     * whether or not the URN itself is valid -- the URN constructor
     * can do that, however.
     *
     * @param requestLine the <tt>String</tt> instance containing the http 
     *  request
     * @return <tt>true</tt> if the request is valid, <tt>false</tt> otherwise
     */
    private static boolean isValidUrnHttpRequest(final String requestLine) {
        return (isValidLength(requestLine) &&
                isValidUriRes(requestLine) &&
                isValidResolutionProtocol(requestLine) && 
                isValidHTTPSpecifier(requestLine));             
    }
    
    /**
     * Returns a <tt>String</tt> containing the URN for the http request.  For
     * a typical SHA1 request, this will return a 41 character URN, including
     * the 32 character hash value.
     *
     * @param requestLine the <tt>String</tt> instance containing the request
     * @return a <tt>String</tt> containing the URN for the http request, or 
     *  <tt>null</tt> if the request could not be read
     */
    private static String extractUrnFromHttpRequest(final String requestLine) {
        int qIndex     = requestLine.indexOf(QUESTION_MARK) + 1;
        int spaceIndex = requestLine.indexOf(SPACE, qIndex);        
        if((qIndex == -1) || (spaceIndex == -1)) {
            return null;
        }
        return requestLine.substring(qIndex, spaceIndex);
    }
    
    /** 
     * Returns whether or not the specified http request meets size 
     * requirements.
     *
     * @param requestLine the <tt>String</tt> instance containing the http request
     * @return <tt>true</tt> if the size of the request line is valid, 
     *  <tt>false</tt> otherwise
     */
    private static final boolean isValidLength(final String requestLine) {
        int size = requestLine.length();
        if((size != 63) && (size != 107)) {
            return false;
        }
        return true;
    }
    
    /**
     * Returns whether or not the http request corresponds with the standard 
     * uri-res request
     *
     * @param requestLine the <tt>String</tt> instance containing the http request
     * @return <tt>true</tt> if the http request includes the standard "uri-res"
     *  (case-insensitive) request, <tt>false</tt> otherwise
     */
    private static final boolean isValidUriRes(final String requestLine) {
        int firstSlash = requestLine.indexOf(SLASH);
        if(firstSlash == -1 || firstSlash == requestLine.length()) {
            return false;
        }
        int secondSlash = requestLine.indexOf(SLASH, firstSlash+1);
        if(secondSlash == -1) {
            return false;
        }
        String uriStr = requestLine.substring(firstSlash+1, secondSlash);
        if(!uriStr.equalsIgnoreCase(HTTPConstants.URI_RES)) {
            return false;
        }
        return true;
    }
    
    /**
     * Returns whether or not the "resolution protocol" for the given URN http
     * line is valid.  We currently only support N2R, which specifies "Given 
     * a URN, return the named resource," and N2X.
     *
     * @param requestLine the <tt>String</tt> instance containing the request
     * @return <tt>true</tt> if the resolution protocol is valid, <tt>false</tt>
     *  otherwise
     */
    private static boolean isValidResolutionProtocol(final String requestLine) {
        int nIndex = requestLine.indexOf(TWO);
        if(nIndex == -1) {
            return false;
        }
        String n2s = requestLine.substring(nIndex-1, nIndex+3);

        // we could add more protocols to this check
        if(!n2s.equalsIgnoreCase(HTTPConstants.NAME_TO_RESOURCE)
           && !n2s.equalsIgnoreCase(HTTPConstants.NAME_TO_THEX)) {
            return false;
        }
        return true;
    }
    /**
     * Returns whether or not the HTTP specifier for the URN http request
     * is valid.
     *
     * @param requestLine the <tt>String</tt> instance containing the http request
     * @return <tt>true</tt> if the HTTP specifier is valid, <tt>false</tt>
     *  otherwise
     */
    private static boolean isValidHTTPSpecifier(final String requestLine) {
        int spaceIndex = requestLine.lastIndexOf(SPACE);
        if(spaceIndex == -1) {
            return false;
        }
        String httpStr = requestLine.substring(spaceIndex+1);
        if(!httpStr.equalsIgnoreCase(HTTPConstants.HTTP10) &&
           !httpStr.equalsIgnoreCase(HTTPConstants.HTTP11)) {
            return false;
        }
        return true;
    }
    
    /**
     * Create a new SHA1 hash string for the specified file on disk.
     *
     * @param file the file to construct the hash from
     * @return the SHA1 hash string
     * @throws <tt>IOException</tt> if there is an error creating the hash
     *  or if the specified algorithm cannot be found
     * @throws <tt>InterruptedException</tt> if the calling thread was 
     *  interrupted while hashing.  (This method can take a while to
     *  execute.)
     */
    public static UrnSet generateUrnsFromFile(final File file) 
      throws IOException, InterruptedException {
        MessageDigest md = new SHA1();

        URN sha1 = URN.generateURN(file, 0, file.length(), Type.SHA1, md,
                SharingSettings.MIN_IDLE_TIME_FOR_FULL_HASHING.getValue(),
                SharingSettings.FRIENDLY_HASHING.getValue());

        UrnSet ret = new UrnSet();
        ret.add(sha1);
        return ret;
    }
    
    /**
     *  Create a new SHA1 hash string for the specified file on disk. This SHA1 
     *  tries to ignore any metadata attached to the file that can be detected, 
     *  producing a SHA1 that should never change over the lifetime of the file. 
     *  The offset is where the SHA1 hash should be started and length is the 
     *  number of bytes that should be read. 
     *  
     *  If no SHA1 could be created or an input value is invalid, null is returned.
     */
    public static URN generateNMS1FromFile(final File file, long offset, long length) throws IOException, InterruptedException {
        MessageDigest md = new SHA1();
        
        return URN.generateURN(file, offset, length, Type.NMSA1, md,
                SharingSettings.MIN_IDLE_TIME_FOR_FULL_HASHING.getValue(),
                SharingSettings.FRIENDLY_HASHING.getValue());
    }
    
    /**
     * Creates a new <tt>URN</tt> instance with a SHA1 hash.
     *
     * @param file the <tt>File</tt> instance to use to create a 
     *  <tt>URN</tt>
     * @return a new <tt>URN</tt> instance
     * @throws <tt>IOException</tt> if there was an error constructing
     *  the <tt>URN</tt>
     * @throws <tt>InterruptedException</tt> if the calling thread was 
     *  interrupted while hashing.  (This method can take a while to
     *  execute.)
     */
    public static URN createSHA1Urn(File file) 
        throws IOException, InterruptedException {
        return generateUrnsFromFile(file).getSHA1();
    }
    
}
