package com.limegroup.bittorrent.handshaking.piecestrategy;

import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;

public class LargestGapStartPieceStrategy extends AbstractPieceStrategy {

    private static final int DEFAULT_MAX_NUM_PIECES = 10;

    private final int maxNumPieces;

    public LargestGapStartPieceStrategy(BTMetaInfo btMetaInfo, BitField interestingPieces) {
        this(btMetaInfo, interestingPieces, DEFAULT_MAX_NUM_PIECES);
    }

    public LargestGapStartPieceStrategy(BTMetaInfo btMetaInfo, BitField interestingPieces,
            int maxNumPieces) {
        super(btMetaInfo, interestingPieces);
        this.maxNumPieces = maxNumPieces;

    }

    public List<BTInterval> getNextPieces() {

        List<BTInterval> nextPieces = new ArrayList<BTInterval>();

        BitField available = getInterestingPieces();

        int currentGapStartIndex = available.nextSetBit(0);
        int lastPieceIndex = -1;
        int currentGapSize = 0;

        int selectedGapStartIndex = 0;
        int selectedGapSize = 0;

        for (int currentPieceIndex = available.nextSetBit(0); currentPieceIndex >= 0; currentPieceIndex = available
                .nextSetBit(currentPieceIndex + 1)) {
            if ((lastPieceIndex + 1) != currentPieceIndex) {
                currentGapStartIndex = currentPieceIndex;
            }

            currentGapSize = currentPieceIndex - currentGapStartIndex + 1;

            if (currentGapSize > selectedGapSize) {
                selectedGapStartIndex = currentGapStartIndex;
                selectedGapSize = currentGapSize;
            }

            if (selectedGapSize >= maxNumPieces) {
                break;
            }
        }

        if (selectedGapSize > 0) {
            int gapEndIndex = selectedGapStartIndex + selectedGapSize;
            for (int gapIndex = selectedGapStartIndex; gapIndex < gapEndIndex; gapIndex++) {
                nextPieces.add(getBtMetaInfo().getPiece(gapIndex));
            }
        }
        return nextPieces;
    }

}
