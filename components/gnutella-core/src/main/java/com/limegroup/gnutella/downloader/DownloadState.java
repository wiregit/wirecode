package com.limegroup.gnutella.downloader;

class DownloadState {
    
    public static final int BEGIN = 0;
    public static final int REQUESTING_THEX = 1;
    public static final int DOWNLOADING_THEX = 2;
    public static final int CONSUMING_BODY = 3;
    public static final int REQUESTING_HTTP = 4;
    public static final int QUEUED = 5;
    public static final int DOWNLOADING = 6;
    
    private int state;
    private boolean http11;
    
    DownloadState() {
        this.state = BEGIN;
        this.http11 = true;
    }
    
    int getCurrentState() {
        return state;
    }
    
    void setState(int state) {
        this.state = state;
    }
    
    boolean isHttp11() {
        return http11;
    }
    
    void setHttp11(boolean http11) {
        this.http11 = http11;
    }
    
    public String toString() {
        switch(state) {
        case BEGIN: return "Begin";
        case REQUESTING_THEX: return "Requesting Thex";
        case DOWNLOADING_THEX: return "Downloading Thex";
        case CONSUMING_BODY: return "Consuming Body";
        case REQUESTING_HTTP: return "Requesting HTTP";
        case QUEUED: return "Queued";
        case DOWNLOADING: return "Downloading";
        default:
            return "Unknown: " + state;
        }
    }

}
