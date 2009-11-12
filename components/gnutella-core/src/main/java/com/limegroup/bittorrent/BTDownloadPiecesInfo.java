package com.limegroup.bittorrent;

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
        
        return LibTorrentPiecesInfo.getPieceState(piecesInfo.charAt(piece));
    }

    @Override
    public int getNumPieces() {
        return piecesInfo.length();
    }
}
