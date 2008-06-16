package com.limegroup.gnutella.caas;

public class SearchParams {

    private String _queryString;
    
    public SearchParams() {
        
    }
    
    public SearchParams(String query) {
        _queryString = query;
    }
    
    public void setQueryString(String s) {
        _queryString = s;
    }
    
    public String getQueryString() {
        return _queryString;
    }
    
    // IMAGES
    //
    // title
    // description
    // artist
    // license type
    // license
    
    // DOCUMENTS
    //
    // title
    // topic
    // author
    // license type
    // license
    
    // AUDIO
    //
    // title
    // artist
    // album
    // genre
    // license type
    // track
    // type
    // year
    // length
    // language
    // bitrate
    // comments
    // copyright
    
    // VIDEO
    //
    // title
    // type
    // year
    // rating
    // length
    // comments
    // license type
    // copyright
    // height
    // width
    // bitrate
    // director
    // studio
    // language
    // stars
    // producer
    // subtitles
    
}
