package org.limewire.libtorrent;

public class LibTorrentException extends RuntimeException {
    private final int type;
    
    public LibTorrentException(WrapperStatus status) {
        super(status.message);
        this.type = status.type;
    }
    
    public int getType() {
        return type;
    }
}
