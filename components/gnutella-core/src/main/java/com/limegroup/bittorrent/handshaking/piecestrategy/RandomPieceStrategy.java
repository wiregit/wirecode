package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;

public class RandomPieceStrategy extends AbstractPieceStrategy {
    public RandomPieceStrategy(BTMetaInfo btMetaInfo, BitField interestingPieces) {
        super(btMetaInfo, interestingPieces);
    }

    public List<BTInterval> getNextPieces() {
        List<BTInterval> pieces = new ArrayList<BTInterval>();
       
        BitField available = getInterestingPieces();

        int selected = getRandomPiece(available);

        if (selected != -1) {
            BTInterval piece = getBtMetaInfo().getPiece(selected);
            pieces.add(piece);
        }
        return pieces;
    }

    protected int getRandomPiece(BitField available) {
        int selectedPiece = -1;
        int current = 1;
        for (int pieceIndex = available.nextSetBit(0); pieceIndex >= 0; pieceIndex = available.nextSetBit(pieceIndex + 1)) {
            if (Math.random() < 1f / current++) {
                selectedPiece = pieceIndex;
            }
        }
        return selectedPiece;
    }

}
