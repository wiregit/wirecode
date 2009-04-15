package org.limewire.libtorrent;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class LibTorrentInfo extends Structure {

    public String sha1;

    public int piece_length;

    public int num_pieces;

    public int num_files;

    public Pointer paths;
    
    private String[] stringPaths;
    
    @Override
    public void read() {
        super.read();
        
        stringPaths = new String[num_files];
        Pointer[] pointers = paths.getPointerArray(0, num_files);
        for(int i = 0; i < num_files; i++) {
            stringPaths[i] = pointers[i].getString(0);
        }
    }
    
}
