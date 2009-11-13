package com.limegroup.gnutella.downloader;

import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.core.api.download.DownloadPiecesInfo;

class GnutellaPieceInfo implements DownloadPiecesInfo {
    
    private final IntervalSet written;
    private final IntervalSet active;
    private final IntervalSet available;
    private final long pieceSize;
    private final int pieceCount;
    
    public GnutellaPieceInfo(IntervalSet written, IntervalSet active, IntervalSet available, long pieceSize, long length) {
        this.written = written;
        this.active = active;
        this.available = available;
        this.pieceSize = pieceSize;
        if(length <= 0) {
            this.pieceCount = 0;
        } else {
            this.pieceCount = (int)Math.min(Integer.MAX_VALUE, length / pieceSize);
        }
    }

    @Override
    public int getNumPieces() {
        return pieceCount;
    }

    @Override
    public PieceState getPieceState(int piece) {
        long pieceStart = piece * pieceSize;
        long pieceEnd = pieceStart + pieceSize;
        Range range = Range.createRange(pieceStart, pieceEnd);
        PieceState state;
        
        if(written.contains(range)) {
            state = PieceState.DOWNLOADED;
        } else if(active.containsAny(range)) {
            state = PieceState.ACTIVE;
        } else if(available.contains(range)) {
            if(written.containsAny(range)) {
                state = PieceState.PARTIAL;
            } else {
                state = PieceState.AVAILABLE;
            }
        } else {
            state = PieceState.UNAVAILABLE;
        }
        
//        System.out.println("Getting piece #" + piece + "(" + pieceStart + ", " + pieceEnd + "], pieceSize: " + pieceSize + ", state: " + state + ", available: " + available);
        
        return state;
        
    }

}
