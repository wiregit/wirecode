package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentPieceState;

import com.sun.jna.Structure;

public class LibTorrentPiecesInfo extends Structure {
    public int numPiecesCompleted;
    public String stateInfo;
    
    private static final char PIECE_DOWNLOADED = 'x';
    private static final char PIECE_PARTIAL = 'p';
    private static final char PIECE_PENDING = '0';
    private static final char PIECE_ACTIVE = 'a';
    private static final char PIECE_UNAVAILABLE = 'u';
    private static final char PIECE_QUEUED = 'q';

    public String getStateInfo() {
        return stateInfo;
    }
    
    public int getNumPiecesCompleted() {
        return numPiecesCompleted;
    }
    
    public static TorrentPieceState getPieceState(char c) {
        switch (c) {
            case PIECE_DOWNLOADED :
                return TorrentPieceState.DOWNLOADED;
            case PIECE_PARTIAL :
                return TorrentPieceState.PARTIAL;
            case PIECE_PENDING :
                return TorrentPieceState.AVAILABLE;
            case PIECE_ACTIVE :
            case PIECE_QUEUED :
                return TorrentPieceState.ACTIVE;
            case PIECE_UNAVAILABLE :
                return TorrentPieceState.UNAVAILABLE;
            default :
                throw new IllegalArgumentException("Unknown Piece Descriptor: " + c);
        }
    }
}
