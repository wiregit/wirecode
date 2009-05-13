package org.limewire.libtorrent;

import java.math.BigInteger;

import com.sun.jna.Structure;

public class LibTorrentInfo extends Structure {

    public String sha1;
    
    public String name;

    public int piece_length;

    public int num_pieces;

    public int num_files;
    
    public String content_length;

    public LibTorrentInfo() {}
    
    public LibTorrentInfo(LibTorrentInfo copy) {
        this.sha1 = copy.sha1;
        this.name = copy.name;
        this.piece_length = copy.piece_length;
        this.num_pieces = copy.num_pieces;
        this.num_files = copy.num_files;
        this.content_length = copy.content_length;
    }

    @Override
    public void read() {
        super.read();
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
