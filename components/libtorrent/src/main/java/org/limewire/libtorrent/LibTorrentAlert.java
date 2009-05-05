package org.limewire.libtorrent;

import com.sun.jna.Structure;

public class LibTorrentAlert extends Structure {
    
    public final static int SAVE_RESUME_DATA_ALERT = 8;
    
    public String sha1;

    public String message;
    
    public String data;

    public int category;
    
    @Override
    public String toString() {
        return sha1 + " " + message  + " [" + category + "] " + data; 
    }
}
