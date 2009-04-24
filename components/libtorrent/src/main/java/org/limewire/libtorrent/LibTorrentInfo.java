package org.limewire.libtorrent;

import java.math.BigInteger;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

public class LibTorrentInfo extends Structure {

    public String sha1;
    
    public String name;

    public int piece_length;

    public int num_pieces;

    public int num_files;
    
    public String content_length;

    public Pointer paths;

    private String[] stringPaths;

    @Override
    public void read() {
        super.read();
        
        stringPaths = new String[num_files];
        Pointer[] pointers = paths.getPointerArray(0, num_files);
        for (int i = 0; i < num_files; i++) {
            stringPaths[i] = pointers[i].getString(0);
        }
    }

    public String[] getPaths() {
        return stringPaths;
    }

    public long getContentLength() {
        if (content_length == null) {
            return -1;
        } else {
            BigInteger contentLength = new BigInteger(content_length);
            return contentLength.longValue();
        }
    }

}
