package org.limewire.bittorrent;

//TODO: convert to interface
public class TorrentPiecesInfo {
    private final String stateInfo;
    private final int numCompleted;
    
    public TorrentPiecesInfo(String stateInfo, int numCompleted) {
        this.stateInfo = stateInfo;
        this.numCompleted = numCompleted;
    }
    
    public String getStateInfo() {
        return stateInfo;
    }
    
    public int getNumCompleted() {
        return numCompleted;
    }
}
