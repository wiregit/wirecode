package com.limegroup.gnutella.caas;

public class SearchParams {

    private String _searchType;
    private String _queryString;
    private String _description;
    private String _artist;
    private String _licenseType;
    private String _license;
    private String _topic;
    private String _author;
    private String _album;
    private String _genre;
    private String _track;
    private String _type;
    private String _year;
    private String _length;
    private String _language;
    private String _bitrate;
    private String _comments;
    private String _copyright;
    private String _rating;
    private String _height;
    private String _width;
    private String _director;
    private String _studio;
    private String _stars;
    private String _producer;
    private String _subtitles;
    private String _publisher;
    private String _platform;
    
    public SearchParams() {
        
    }
    
    public SearchParams(String query) {
        _queryString = query;
    }
    
    
    
    
    
    public void setSearchType(String s) {
        _searchType = s;
    }
    
    public void setQueryString(String s) {
        _queryString = s;
    }
    
    public void setTitle(String s) {
        _queryString = s;
    }
    
    public void setDescription(String s) {
        _description = s;
    }
    
    public void setArtist(String s) {
        _artist = s;
    }
    
    public void setLicenseType(String s) {
        _licenseType = s;
    }
    
    public void setLicense(String s) {
        _license = s;
    }
    
    public void setTopic(String s) {
        _topic = s;
    }
    
    public void setAuthor(String s) {
        _author = s;
    }
    
    public void setAlbum(String s) {
        _album = s;
    }
    
    public void setGenre(String s) {
        _genre = s;
    }
    
    public void setTrack(String s) {
        _track = s;
    }
    
    public void setType(String s) {
        _type = s;
    }
    
    public void setYear(String s) {
        _year = s;
    }
    
    public void setLength(String s) {
        _length = s;
    }
    
    public void setLanguage(String s) {
        _language = s;
    }
    
    public void setBitrate(String s) {
        _bitrate = s;
    }
    
    public void setComments(String s) {
        _comments = s;
    }
    
    public void setCopyright(String s) {
        _copyright = s;
    }
    
    public void setRating(String s) {
        _rating = s;
    }
    
    public void setHeight(String s) {
        _height = s;
    }
    
    public void setWidth(String s) {
        _width = s;
    }
    
    public void setDirector(String s) {
        _director = s;
    }
    
    public void setStudio(String s) {
        _studio = s;
    }
    
    public void setStars(String s) {
        _stars = s;
    }
    
    public void setProducer(String s) {
        _producer = s;
    }
    
    public void setSubtitles(String s) {
        _subtitles = s;
    }
    
    public void setPublisher(String s) {
        _publisher = s;
    }
    
    public void setPlatform(String s) {
        _platform = s;
    }
    
    
    
    
    
    public String getSearchType() {
        return _searchType;
    }
    
    public String getQueryString() {
        return _queryString;
    }
    
    public String getTitle() {
        return _queryString;
    }
    
    public String getDescription() {
        return _description;
    }
    
    public String getArtist() {
        return _artist;
    }
    
    public String getLicenseType() {
        return _licenseType;
    }
    
    public String getLicense() {
        return _license;
    }
    
    public String getTopic() {
        return _topic;
    }
    
    public String getAuthor() {
        return _author;
    }
    
    public String getAlbum() {
        return _album;
    }
    
    public String getGenre() {
        return _genre;
    }
    
    public String getTrack() {
        return _track;
    }
    
    public String getType() {
        return _type;
    }
    
    public String getYear() {
        return _year;
    }
    
    public String getLength() {
        return _length;
    }
    
    public String getLanguage() {
        return _language;
    }
    
    public String getBitrate() {
        return _bitrate;
    }
    
    public String getComments() {
        return _comments;
    }
    
    public String getCopyright() {
        return _copyright;
    }
    
    public String getRating() {
        return _rating;
    }
    
    public String getHeight() {
        return _height;
    }
    
    public String getWidth() {
        return _width;
    }
    
    public String getDirector() {
        return _director;
    }
    
    public String getStudio() {
        return _studio;
    }
    
    public String getStars() {
        return _stars;
    }
    
    public String getProducer() {
        return _producer;
    }
    
    public String getSubtitles() {
        return _subtitles;
    }
    
    public String getPublisher() {
        return _publisher;
    }
    
    public String getPlatform() {
        return _platform;
    }
    
    // IMAGES
    //
    // *title
    // description
    // artist
    // license type
    // license
    
    // DOCUMENTS
    //
    // *title
    // topic
    // author
    // license type
    // license
    
    // AUDIO
    //
    // *title
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
    // *title
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
