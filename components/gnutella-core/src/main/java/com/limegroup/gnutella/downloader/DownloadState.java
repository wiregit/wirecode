package com.limegroup.gnutella.downloader;

class DownloadState {
    
    public static final int BEGIN = 0;
    public static final int REQUESTING_THEX = 1;
    public static final int CONSUMING_BODY = 2;
    public static final int REQUESTING_HTTP = 3;
    public static final int QUEUED = 4;
    public static final int DOWNLOADING = 5;
    
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

}
