package com.limegroup.bittorrent;

import org.limewire.bittorrent.TorrentPieceState;
import org.limewire.core.api.download.DownloadPiecesInfo;
import org.limewire.libtorrent.LibTorrentPiecesInfo;

class BTDownloadPiecesInfo implements DownloadPiecesInfo {

    private final String piecesInfo;
    
    BTDownloadPiecesInfo(String piecesInfo) {
        this.piecesInfo = piecesInfo;
    }
    
    @Override
    public PieceState getPieceState(int piece) {
        if (piece >= piecesInfo.length()) {
            if (piecesInfo.length() == 0) {
                return PieceState.DOWNLOADED;
            }
            else {
                return PieceState.UNAVAILABLE;
            }
        }
        
        return convertPieceState(LibTorrentPiecesInfo.getPieceState(piecesInfo.charAt(piece)));
    }

    @Override
    public int getNumPieces() {
        return piecesInfo.length();
    }
    
    private static PieceState convertPieceState(TorrentPieceState state) {
        switch(state) {
            case ACTIVE :
                return PieceState.ACTIVE;
            case DOWNLOADED :
                return PieceState.DOWNLOADED;
            case PARTIAL :
                return PieceState.PARTIAL;
            case AVAILABLE :
                return PieceState.AVAILABLE;
            case UNAVAILABLE :
                return PieceState.UNAVAILABLE;
            default:
                throw new IllegalArgumentException("Unknown TorrentPieceState: " + state);
        }
    }
}
