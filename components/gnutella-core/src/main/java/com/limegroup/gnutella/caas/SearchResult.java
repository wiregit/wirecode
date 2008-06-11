package com.limegroup.gnutella.caas;

import java.util.Set;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;

public interface SearchResult {

    /**
     * 
     */
    public String getHost();
    
    /**
     * 
     */
    public int getPort();
    
    /**
     * 
     */
    public long getIndex();
    
    /**
     * 
     */
    public String getFilename();
    
    /**
     * 
     */
    public long getSize();
    
    /**
     * 
     */
    public GUID getClientGUID();
    
    /**
     * 
     */
    public int getSpeed();
    
    /**
     * 
     */
    public boolean getChat();
    
    /**
     * 
     */
    public int getQuality();
    
    /**
     * 
     */
    public boolean getBrowseHost();
    
    /**
     * 
     */
    public Set<URN> getURNs();
    
    /**
     * 
     */
    public boolean getReplyToMulticast();
    
    /**
     * 
     */
    public boolean getFirewalled();
    
    /**
     * 
     */
    public String getVendor();
    
    /**
     * 
     */
    public long getCreateTime();
    
    /**
     * 
     */
    public boolean getTlsCapable();
    
    /**
     * 
     */
    public boolean getHttp11();

    /**
     * 
     */
    public URN getSha1Urn();
    
}
