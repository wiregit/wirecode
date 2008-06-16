package com.limegroup.gnutella.caas.restlet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.limegroup.gnutella.caas.Download;
import com.limegroup.gnutella.caas.SearchResult;

public class RestletDownload implements Download {

    private final RestletSearchResult _searchResult;
    private String _downloadId;
    private Element _downloadItem;
    private String _error;
    
    public RestletDownload(SearchResult sr) {
        _searchResult = (RestletSearchResult)sr;
    }
    
    public boolean start() {
        Document document = RestletConnector.sendRequest("/download", _searchResult.getSearchItem());
        Element downloads = document.getDocumentElement();
        NodeList downloadList = downloads.getElementsByTagName("download");
        
        System.out.println("RestletDownload::start().. downloadList.getLength = " + downloadList.getLength());
        
        if (downloadList.getLength() == 0)
            return false;
        
        _error = downloadList.item(0).getAttributes().getNamedItem("error").getTextContent();
        _downloadId = downloadList.item(0).getAttributes().getNamedItem("id").getTextContent();
        
        if (_error != null && _error.length() == 0) {
            System.err.println("RestletDownload::start().. error: " + _error);
            return false;
        }
        
        return true;
    }
    
    public boolean stop() {
        Document document = RestletConnector.sendRequest("/download/stop/" + _downloadId);
        Element downloads = document.getDocumentElement();
        NodeList downloadList = downloads.getElementsByTagName("download");
        
        System.out.println("RestletDownload::stop().. downloadList.getLength = " + downloadList.getLength());
        
        if (downloadList.getLength() == 0)
            return false;
        
        _error = downloadList.item(0).getAttributes().getNamedItem("error").getTextContent();
        
        if (_error != null && _error.length() == 0) {
            System.err.println("RestletDownload::stop().. error: " + _error);
            return false;
        }
        
        return true;
    }
    
    public boolean pause() {
        Document document = RestletConnector.sendRequest("/download/pause/" + _downloadId);
        Element downloads = document.getDocumentElement();
        NodeList downloadList = downloads.getElementsByTagName("download");
        
        System.out.println("RestletDownload::pause().. downloadList.getLength = " + downloadList.getLength());
        
        if (downloadList.getLength() == 0)
            return false;
        
        _error = downloadList.item(0).getAttributes().getNamedItem("error").getTextContent();
        
        if (_error != null && _error.length() == 0) {
            System.err.println("RestletDownload::pause().. error: " + _error);
            return false;
        }
        
        return true;
    }
    
    public boolean resume() {
        Document document = RestletConnector.sendRequest("/download/resume/" + _downloadId);
        Element downloads = document.getDocumentElement();
        NodeList downloadList = downloads.getElementsByTagName("download");
        
        System.out.println("RestletDownload::resume().. downloadList.getLength = " + downloadList.getLength());
        
        if (downloadList.getLength() == 0)
            return false;
        
        _error = downloadList.item(0).getAttributes().getNamedItem("error").getTextContent();
        
        if (_error != null && _error.length() == 0) {
            System.err.println("RestletDownload::resume().. error: " + _error);
            return false;
        }
        
        return true;
    }
    
    public void update() {
        Document document = RestletConnector.sendRequest("/download/" + _downloadId);
        Element downloads = document.getDocumentElement();
        NodeList downloadList = downloads.getElementsByTagName("download");
        
        for (int i = 0; i < downloadList.getLength(); ++i) {
            Element download = (Element)downloadList.item(i);
            
            if (!_downloadId.equals(download.getAttribute("id")))
                continue;
            
            _downloadItem = download;
        }
    }
    
    public void addSource(SearchResult sr) {
        
    }
    
    public long getAmountRead() {
        return Long.parseLong(getValue("amount_read"));
    }
    
    public int getAmountPending() {
        return Integer.parseInt(getValue("amount_pending"));
    }
    
    public long getAmountVerified() {
        return Long.parseLong(getValue("amount_verified"));
    }
    
    public boolean isComplete() {
        return Boolean.parseBoolean(getValue("is_complete"));
    }
    
    public String getFilename() {
        return getValue("filename");
    }
    
    public String getState() {
        return getValue("state");
    }
    
    /**
     * 
     */
    private String getValue(String name) {
        NodeList list = _downloadItem.getElementsByTagName(name);
        
        if (0 == list.getLength())
            return null;
        
        return list.item(0).getTextContent();
    }

}
