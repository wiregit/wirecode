package com.limegroup.bittorrent.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.limewire.collection.BitField;
import org.limewire.collection.BitFieldSet;
import org.limewire.collection.BitSet;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.AbstractSwarmCoordinator;
import org.limewire.swarm.SwarmBlockSelector;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.TorrentFile;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.bittorrent.disk.TorrentDiskManager;

public class BTSwarmCoordinator extends AbstractSwarmCoordinator {

    private final TorrentFileSystem torrentFileSystem;

    private final TorrentDiskManager torrentDiskManager;

    private final BTMetaInfo btMetaInfo;

    public BTSwarmCoordinator(BTMetaInfo btMetaInfo, TorrentFileSystem torrentFileSystem,
            TorrentDiskManager torrentDiskManager) {
        assert btMetaInfo != null;
        assert torrentFileSystem != null;
        assert torrentDiskManager != null;
        this.btMetaInfo = btMetaInfo;
        this.torrentFileSystem = torrentFileSystem;
        this.torrentDiskManager = torrentDiskManager;
    }

    public SwarmWriteJob createWriteJob(Range range, SwarmWriteJobControl callback) {
        List<BTInterval> pieces = createBTInterval(range);
        return new BTSwarmWriteJob(pieces, torrentDiskManager, callback);
    }

    public void finish() throws IOException {
        // do nothing let the managed_torrent manage closing things

    }

    public long getAmountLost() {
        return torrentDiskManager.getNumCorruptedBytes();
    }

    public long getAmountVerified() {
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.getAmountVerified() is not implemented.");
    }

    public SwarmFile getSwarmFile(Range range) {
        long position = range.getLow();
        List<TorrentFile> torrentFiles = torrentFileSystem.getFiles();
        for (TorrentFile torrentFile : torrentFiles) {
            long startByte = torrentFile.getStartByte();
            long endByte = torrentFile.getEndByte();
            if (position >= startByte && position <= endByte) {
                return new BTSwarmFile(torrentFile);
            }
        }
        return null;
    }

    public boolean isComplete() {
        return torrentDiskManager.isComplete();
    }

    public Range lease() {
        throw new UnsupportedOperationException("BTSwarmCoordinator.lease() is not implemented.");
    }

    public Range leasePortion(IntervalSet availableRanges) {
        // TODO do not lease random, use a gap strategy to find a good gap.
        // This will be multiple ranges in the long run

        int numPieces = btMetaInfo.getNumBlocks();

        BitSet avalableBitSet = new BitSet(numPieces);

        avalableBitSet.flip(0, numPieces);

        BitField availableRangesBitField = new BitFieldSet(avalableBitSet, numPieces);
        List<BTInterval> leased = torrentDiskManager.lease(availableRangesBitField, null, null);

        Range lease = null;
        if (leased != null && leased.size() > 0) {
            BTInterval firstBlock = leased.get(0);
            long startByte = getLow(firstBlock);
            long endByte = getHigh(leased.get(leased.size() - 1));
            lease = Range.createRange(startByte, endByte);
        }

        return lease;
    }

    public Range leasePortion(IntervalSet availableRanges, SwarmBlockSelector selector) {
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.leasePortion(IntervalSet availableRanges, SwarmBlockSelector selector) is not implemented.");
    }

    public void pending(Range range) {
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.pending(Range range) is not implemented.");
    }

    public Range renewLease(Range oldLease, Range newLease) {

        List<BTInterval> oldInterval = createBTInterval(oldLease);
        List<BTInterval> newInterval = createBTInterval(newLease);
        torrentDiskManager.renewLease(oldInterval, newInterval);
        return newLease;
    }

    public void reverify() {
        throw new UnsupportedOperationException("BTSwarmCoordinator.reverify() is not implemented.");

    }

    public void unlease(Range range) {
        List<BTInterval> pieces = createBTInterval(range);
        for (BTInterval btInterval : pieces) {
            torrentDiskManager.releaseInterval(btInterval);
        }
    }

    /**
     * 
     * 
     * @param range
     * @return
     */
    private List<BTInterval> createBTInterval(Range range) {
        List<BTInterval> btIntervals = new ArrayList<BTInterval>();
        long index = range.getLow();
        while (index <= range.getHigh()) {
            BTInterval piece = btMetaInfo.getPieceAt(index);
            
            long byteLow = getLow(piece);
            long offsetLow = 0;
            
            if (byteLow < range.getLow()) {
                offsetLow = byteLow - range.getLow();
                byteLow = range.getLow();
            }
           long byteHigh = getHigh(piece);
           long offsetHigh = 0;
            if (byteHigh > range.getHigh()) {
                offsetHigh = byteHigh - range.getHigh();
                byteHigh = range.getHigh();
            }
            
            
            long pieceLow = piece.getLow() - offsetLow;
            long pieceHigh = piece.getHigh() - offsetHigh;
            
            piece = new BTInterval(pieceLow, pieceHigh, piece.getBlockId());
            btIntervals.add(piece);
            index = byteHigh + 1;
        }
        return btIntervals;
    }

    private long getHigh(BTInterval piece) {
        long pieceNum = piece.getBlockId();
        long high = piece.getHigh() + pieceNum * btMetaInfo.getPieceLength();
        return high;
    }

    private long getLow(BTInterval piece) {
        long pieceNum = piece.getBlockId();
        long low = piece.getLow() + pieceNum * btMetaInfo.getPieceLength();
        return low;
    }

    public void unpending(Range range) {
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.unpending(Range) is not implemented.");

    }

    public void verify() {
        throw new UnsupportedOperationException("BTSwarmCoordinator.verify() is not implemented.");

    }

    public long write(Range range, ByteBuffer content) throws IOException {
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.write(Range, ByteBuffer) is not implemented.");

    }

    public void wrote(Range range) {
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.wrote(Range) is not implemented.");
    }

}
