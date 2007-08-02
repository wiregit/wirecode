package com.limegroup.store;

import static com.limegroup.gnutella.Constants.MAX_FILE_SIZE;

import java.io.Serializable;
import java.net.URL;

import com.limegroup.gnutella.URN;

/**
 * Contains content for generating a download from the LimeWire Store (LWS)
 *
 */
public class StoreDescriptor implements Serializable {

    /**
     * Location to download file from
     */
    private final URL url;
    
    /**
     * SHA1 hash of the file to download
     */
    private final URN urn;
    
    /**
     * Name to save the file as
     */
    private final String fileName;
    
    /**
     * Number of bytes of the file to be downloaded
     */
    private final long size;
  
    
    public StoreDescriptor( URL url, URN urn, String fileName, long size ){
      
        if( url == null ) {
            throw new NullPointerException("null url");
        }
        if( urn == null ) {
            throw new NullPointerException("null sha hash");
        }
        if(fileName == null) {
            throw new NullPointerException("null filename");
        }
        if(fileName.equals("")) {
            throw new IllegalArgumentException("cannot accept empty string file name");
        }
        if((size < 0 || size > MAX_FILE_SIZE) ) {
            throw new IllegalArgumentException("invalid size: "+size);
        }
        
        this.url = url;
        this.urn = urn;
        this.fileName = fileName;
        this.size = size;
    }
    
    public URL getURL() {
        return url;
    }
    
    public final String getFileName(){
        return fileName;
    }
    
    public final URN getSHA1Urn(){
        return urn;
    }
    
    public final long getSize(){
        return size;
    }
    
    public String toString(){
        return url + ":" + urn.toString() + ":" + size + ":" + fileName;
    }
}
