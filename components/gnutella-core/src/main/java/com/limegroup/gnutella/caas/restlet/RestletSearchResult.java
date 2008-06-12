package com.limegroup.gnutella.caas.restlet;

import java.io.IOException;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.caas.Common;
import com.limegroup.gnutella.caas.SearchResult;

public class RestletSearchResult implements SearchResult{

    private final Element _searchItem;
    
    /**
     * 
     */
    Element getSearchItem() {
        return _searchItem;
    }
    
    /**
     * 
     */
    public RestletSearchResult(Element searchItem) {
        _searchItem = searchItem;
    }
    
    public String getHost() {
        return getValue("host");
    }
    
    public int getPort() {
        return Integer.parseInt(getValue("port"));
    }
    
    public long getIndex() {
        return Long.parseLong(getValue("index"));
    }
    
    public String getFilename() {
        return getValue("filename");
    }
    
    public long getSize() {
        return Long.parseLong(getValue("size"));
    }
    
    public GUID getClientGUID() {
        return new GUID(getValue("guid"));
    }
    
    public int getSpeed() {
        return Integer.parseInt(getValue("speed"));
    }
    
    public boolean getChat() {
        return Boolean.parseBoolean(getValue("chat"));
    }
    
    public int getQuality() {
        return Integer.parseInt(getValue("quality"));
    }
    
    public boolean getBrowseHost() {
        return Boolean.parseBoolean(getValue("browseHost"));
    }
    
    public Set<URN> getURNs() {
        return Common.stringToURNs(getValue("urns"));
    }
    
    public boolean getReplyToMulticast() {
        return Boolean.parseBoolean(getValue("replyToMulticast"));
    }
    
    public boolean getFirewalled() {
        return Boolean.parseBoolean(getValue("firewalled"));
    }
    
    public String getVendor() {
        return getValue("vendor");
    }
    
    public long getCreateTime() {
        return Long.parseLong(getValue("createTime"));
    }
    
    public boolean getTlsCapable() {
        return Boolean.parseBoolean(getValue("tlsCapable"));
    }
    
    public boolean getHttp11() {
        return Boolean.parseBoolean(getValue("http11"));
    }
    
    public URN getSha1Urn() {
        URN urn = null;
        
        try {
            urn = URN.createSHA1Urn(getValue("sha1urn"));
        }
        catch (IOException e) {
            System.err.println("ResultSearchResult::getSha1Urn().. " + e.getMessage());
            e.printStackTrace();
        }
        
        return urn;
    }
    
    /**
     * 
     */
    private String getValue(String name) {
        NodeList list = _searchItem.getElementsByTagName(name);
        
        if (0 == list.getLength())
            return null;
        
        return list.item(0).getTextContent();
    }
}
