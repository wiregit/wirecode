package com.limegroup.bittorrent.handshaking.piecestrategy;

import org.limewire.collection.BitField;

import com.limegroup.bittorrent.BTMetaInfo;

public abstract class AbstractPieceStrategy implements PieceStrategy{

    private final BTMetaInfo btMetaInfo;

    private final BitField interestingPieces;

    public AbstractPieceStrategy(BTMetaInfo btMetaInfo, BitField interestingPieces) {
        this.btMetaInfo = btMetaInfo;
        this.interestingPieces = interestingPieces;
    }

    public BitField getInterestingPieces() {
        return interestingPieces;
    }

    public BTMetaInfo getBtMetaInfo() {
        return btMetaInfo;
    }

}