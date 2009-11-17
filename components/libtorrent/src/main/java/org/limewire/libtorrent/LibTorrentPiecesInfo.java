package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentPieceState;
import org.limewire.bittorrent.TorrentPiecesInfo;

/**
 * Lightweight impl of TorrentPiecesInfo that copies its working data from
 *  a LibTorrentPiecesInfoContainer returned from the JNA.
 */
class LibTorrentPiecesInfo implements TorrentPiecesInfo {

    private static final char PIECE_DOWNLOADED = 'x';
    private static final char PIECE_PARTIAL = 'p';
    private static final char PIECE_PENDING = '0';
    private static final char PIECE_ACTIVE = 'a';
    private static final char PIECE_UNAVAILABLE = 'u';
    private static final char PIECE_QUEUED = 'q';
    
    private final String stateInfo;
    private final int numPiecesCompleted;
    
    /**
     * Generates a working instance of {@link TorrentPiecesInfo} from a 
     *  {@link LibTorrentPiecesInfoContainer} returned from libtorrent through
     *  JNA.
     *       
     * <p> NOTE: Copies the payload from the original container so it may safely be disposed of.  
     */
    LibTorrentPiecesInfo(LibTorrentPiecesInfoContainer piecesInfoContainer) {
        stateInfo = new String(piecesInfoContainer.getStateInfo());
        numPiecesCompleted = piecesInfoContainer.getNumPiecesCompleted();
    }
    
    @Override
    public int getNumPieces() {
        return stateInfo.length();
    }

    @Override
    public int getNumPiecesCompleted() {
        return numPiecesCompleted;
    }

    @Override
    public TorrentPieceState getPieceState(int piece) {
        return getPieceState(stateInfo.charAt(piece));
    }
    
    private static TorrentPieceState getPieceState(char c) {
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
