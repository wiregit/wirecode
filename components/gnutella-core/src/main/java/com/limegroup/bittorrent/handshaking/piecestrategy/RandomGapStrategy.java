package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;

public class RandomGapStrategy extends RandomPieceStrategy {
    private int max = 10;

    public RandomGapStrategy(BTMetaInfo btMetaInfo, BitField interestingPieces) {
        super(btMetaInfo, interestingPieces);
    }

    public List<BTInterval> getNextPieces() {
        List<BTInterval> nextPieces = new ArrayList<BTInterval>();

        int nextPiece = getRandomPiece(getInterestingPieces());
        if (nextPiece != -1) {
            do {
                if (getInterestingPieces().get(nextPiece)) {
                    BTInterval btInterval = getBtMetaInfo().getPiece(nextPiece);
                    nextPieces.add(btInterval);
                } else {
                    break;
                }
                nextPiece++;
                if (nextPieces.size() >= max || nextPiece >= getBtMetaInfo().getNumBlocks()) {
                    break;
                }
            } while (true);
        }

        return nextPieces;
    }
}
