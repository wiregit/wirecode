package com.limegroup.gnutella.websearch;


public class WebResult {
    
    private final String _title;
    private final String _description;
    private final String _bidPrice;
    private final String _clickURL;

    public WebResult(String title, String desc, String bid, String click) {
        _title = title;
        _description = desc;
        _bidPrice = bid;
        _clickURL = click;
    }    

    public String toString() {
        return _title+", "+_description+", "+_bidPrice+", "+_clickURL;
    }
    
    public String getTitle() {
        return _title;
    }

    public String getDescription() {
        return _description;
    }

    public String getBidPrice() {
        return _bidPrice;
    }

    public String getClickUrl() {
        return _clickURL;
    }


    
}
