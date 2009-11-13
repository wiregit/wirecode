package com.limegroup.bittorrent;

import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentPieceState;
import org.limewire.bittorrent.TorrentPiecesInfo;
import org.limewire.core.api.download.DownloadPiecesInfo;
import org.limewire.libtorrent.LibTorrentPiecesInfo;

// TODO: move all the string stuff inside libtorrent.
class BTDownloadPiecesInfo implements DownloadPiecesInfo {

    private final TorrentPiecesInfo piecesInfo;
    private final Torrent torrent;
    
    BTDownloadPiecesInfo(Torrent torrent) {
        this.torrent = torrent;
        
        piecesInfo = torrent.getPiecesInfo();
    }
    
    @Override
    public PieceState getPieceState(int piece) {
        
        String stateInfo = piecesInfo.getStateInfo();
        
        if (piece >= stateInfo.length()) {
            // TODO: this is so things show up all completed when a download is completed.
            //        should be cleaned up to not rely on this strange behaviour to detect
            //        completion.
            if (stateInfo.length() == 0) {
                return PieceState.DOWNLOADED;
            }
            else {
                return PieceState.UNAVAILABLE;
            }
        }
        
        return convertPieceState(LibTorrentPiecesInfo.getPieceState(stateInfo.charAt(piece)));
    }

    @Override
    public int getNumPieces() {
        return piecesInfo.getStateInfo().length();
    }

    @Override
    public long getPieceSize() {
        return torrent.getTorrentInfo().getPieceLength();
    }  
    
    @Override
    public int getNumPiecesCompleted() {
        // TODO: See above todo
        if (piecesInfo.getStateInfo().length() != 0) {
            return piecesInfo.getNumCompleted();
        } 
        else {
            // TODO: ...
            return -2;
        }
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
