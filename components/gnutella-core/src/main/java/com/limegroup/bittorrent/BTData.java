package com.limegroup.bittorrent;

import java.util.List;
import java.util.Set;


/** Defines an interface from which all data about a .torrent file can be retrieved. */
public interface BTData {

    /** A structure for storing information about files within the .torrent. */
    public static class BTFileData {
        private final Long length;
        private final String path;
        
        BTFileData(Long length, String path) {
            this.length = length;
            this.path = path;
        }
    
        public Long getLength() {
            return length;
        }
    
        public String getPath() {
            return path;
        }
    }

    public String getAnnounce();

    public List<BTData.BTFileData> getFiles();

    public Set<String> getFolders();

    public boolean isPrivate();

    public byte[] getInfoHash();

    public Long getLength();

    public String getName();

    public Long getPieceLength();

    public byte[] getPieces();

    public void clearPieces();

}