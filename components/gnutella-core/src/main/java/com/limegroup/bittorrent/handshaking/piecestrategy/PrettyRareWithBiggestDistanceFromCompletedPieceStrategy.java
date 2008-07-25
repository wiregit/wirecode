package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTLinkManager;
import com.limegroup.bittorrent.BTMetaInfo;

public class PrettyRareWithBiggestDistanceFromCompletedPieceStrategy extends
        RarestPieceFirstStrategy {

    public PrettyRareWithBiggestDistanceFromCompletedPieceStrategy(BTMetaInfo btMetaInfo,
            BTLinkManager btLinkManager, BitField interestingPieces) {
        super(btMetaInfo, btLinkManager, interestingPieces);
    }

    @Override
    public List<BTInterval> getNextPieces() {
        List<BTInterval> nextPieces = new ArrayList<BTInterval>();
        int nextPiece = getPrettyRarePiece();
        if (nextPiece > -1) {
            nextPieces.add(getBtMetaInfo().getPiece(nextPiece));
        }
        return nextPieces;
    }

    public int getPrettyRarePiece() {
        int numPeers = getBTLinkManager().getNumConnections();
        int numPieces = getBtMetaInfo().getNumBlocks();

        int X = (int) (Math.sqrt(numPeers) - 1);
        int gap = 0;
        int currGap = 0;
        int nextPiece = -1;

        int currentRareness = Integer.MAX_VALUE;

        for (int pieceIndex = 0; pieceIndex < numPieces; pieceIndex++) {
            gap++;
            int pieceRareness = getPieceRareness(pieceIndex, Integer.MAX_VALUE);
            boolean peerHasPiece = pieceRareness != 0;
            if (peerHasPiece) {
                if (pieceRareness < (currentRareness - X) || pieceRareness <= (currentRareness + X)
                        && gap > currGap) {
                    currentRareness = pieceRareness;
                    currGap = gap;
                    nextPiece = pieceIndex;
                }
            } else {
                gap = 0;
            }
        }
        return nextPiece;
    }
}
