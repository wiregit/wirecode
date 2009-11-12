package org.limewire.libtorrent;

import org.limewire.core.api.download.DownloadPiecesInfo.PieceState;

import com.sun.jna.Structure;

public class LibTorrentPiecesInfo extends Structure implements Structure.ByReference {
    public String piecesInfo;
    
    private static final char PIECE_DOWNLOADED = 'x';
    private static final char PIECE_PARTIAL = 'p';
    private static final char PIECE_PENDING = '0';
    private static final char PIECE_ACTIVE = 'a';
    private static final char PIECE_UNAVAILABLE = 'u';
    private static final char PIECE_QUEUED = 'q';
    
    public static PieceState getPieceState(char c) {
        switch (c) {
            case PIECE_DOWNLOADED :
                return PieceState.DOWNLOADED;
            case PIECE_PARTIAL :
                return PieceState.PARTIAL;
            case PIECE_PENDING :
                return PieceState.PENDING;
            case PIECE_ACTIVE :
            case PIECE_QUEUED :
                return PieceState.ACTIVE;
            case PIECE_UNAVAILABLE :
                return PieceState.UNAVAILABLE;
            default :
                throw new IllegalArgumentException("Unknown Piece Descriptor: " + c);
        }
    }
}
