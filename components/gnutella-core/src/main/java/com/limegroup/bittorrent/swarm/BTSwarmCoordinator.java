package com.limegroup.bittorrent.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.collection.BitField;
import org.limewire.collection.BitFieldSet;
import org.limewire.collection.BitSet;
import org.limewire.collection.IntervalSet;
import org.limewire.collection.Range;
import org.limewire.swarm.SwarmBlockSelector;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;
import org.limewire.swarm.impl.AbstractSwarmCoordinator;
import org.limewire.util.Objects;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.bittorrent.TorrentFile;
import com.limegroup.bittorrent.TorrentFileSystem;
import com.limegroup.bittorrent.disk.TorrentDiskManager;
import com.limegroup.bittorrent.handshaking.piecestrategy.PieceStrategy;
import com.limegroup.bittorrent.handshaking.piecestrategy.RandomPieceStrategy;

public class BTSwarmCoordinator extends AbstractSwarmCoordinator {

    private final TorrentFileSystem torrentFileSystem;

    private final TorrentDiskManager torrentDiskManager;

    private final BTMetaInfo btMetaInfo;

    private final PieceStrategy pieceStrategy;

    private final Set<BTInterval> requested;

    public BTSwarmCoordinator(BTMetaInfo btMetaInfo, TorrentFileSystem torrentFileSystem,
            TorrentDiskManager torrentDiskManager) {
        this(btMetaInfo, torrentFileSystem, torrentDiskManager, new RandomPieceStrategy(btMetaInfo));
    }

    public BTSwarmCoordinator(BTMetaInfo btMetaInfo, TorrentFileSystem torrentFileSystem,
            TorrentDiskManager torrentDiskManager, PieceStrategy pieceStrategy) {
        this.btMetaInfo = Objects.nonNull(btMetaInfo, "btMetaInfo");
        this.torrentFileSystem = Objects.nonNull(torrentFileSystem, "torrentFileSystem");
        this.torrentDiskManager = Objects.nonNull(torrentDiskManager, "torrentDiskManager");
        this.pieceStrategy = Objects.nonNull(pieceStrategy, "pieceStrategy");
        this.requested = Collections.synchronizedSet(new HashSet<BTInterval>());
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

    public Range leasePortion(IntervalSet availableRanges) {
        int numPieces = btMetaInfo.getNumBlocks();

        BitSet avalableBitSet = new BitSet(numPieces);

        avalableBitSet.flip(0, numPieces);

        BitField availableRangesBitField = new BitFieldSet(avalableBitSet, numPieces);
        List<BTInterval> leased = torrentDiskManager.lease(availableRangesBitField, requested,
                pieceStrategy);

        Range lease = null;
        if (leased != null && leased.size() > 0) {
            requested.addAll(leased);
            BTInterval firstBlock = leased.get(0);
            long startByte = btMetaInfo.getLowByte(firstBlock);
            long endByte = btMetaInfo.getHighByte(leased.get(leased.size() - 1));
            lease = Range.createRange(startByte, endByte);
        }

        return lease;
    }

    public Range renewLease(Range oldLease, Range newLease) {
        List<BTInterval> oldInterval = createBTInterval(oldLease);
        List<BTInterval> newInterval = createBTInterval(newLease);
        torrentDiskManager.renewLease(oldInterval, newInterval);
        requested.removeAll(oldInterval);
        requested.addAll(newInterval);
        return newLease;
    }

    public void unlease(Range range) {
        List<BTInterval> pieces = createBTInterval(range);
        for (BTInterval btInterval : pieces) {
            torrentDiskManager.releaseInterval(btInterval);
            requested.remove(btInterval);
        }
    }

    /**
     * Takes a range and converts it into a List of BTInterval objects.
     * 
     * @param range
     * @return
     */
    private List<BTInterval> createBTInterval(Range range) {
        List<BTInterval> btIntervals = new ArrayList<BTInterval>();
        long index = range.getLow();
        while (index <= range.getHigh()) {
            BTInterval piece = btMetaInfo.getPieceAt(index);

            long byteLow = btMetaInfo.getLowByte(piece);
            long offsetLow = 0;
            if (byteLow < range.getLow()) {
                offsetLow = byteLow - range.getLow();
                byteLow = range.getLow();
            }

            long byteHigh = btMetaInfo.getHighByte(piece);
            long offsetHigh = 0;
            if (byteHigh > range.getHigh()) {
                offsetHigh = byteHigh - range.getHigh();
                byteHigh = range.getHigh();
            }

            // rebuild piece using offsets
            long pieceLow = piece.getLow() - offsetLow;
            long pieceHigh = piece.getHigh() - offsetHigh;
            piece = new BTInterval(pieceLow, pieceHigh, piece.getBlockId());

            btIntervals.add(piece);
            index = byteHigh + 1;
        }
        return btIntervals;
    }

    public long getAmountVerified() {
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.getAmountVerified() is not implemented.");
    }

    public Range lease() {
        // not the job of the BTSwarmCoordinator
        throw new UnsupportedOperationException("BTSwarmCoordinator.lease() is not implemented.");
    }

    public void unpending(Range range) {
        // not the job of the BTSwarmCoordinator
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.unpending(Range) is not implemented.");
    }

    public Range leasePortion(IntervalSet availableRanges, SwarmBlockSelector selector) {
        // not the job of the BTSwarmCoordinator
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.leasePortion(IntervalSet availableRanges, SwarmBlockSelector selector) is not implemented.");
    }

    public void pending(Range range) {
        // not the job of the BTSwarmCoordinator
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.pending(Range range) is not implemented.");
    }

    public void verify() {
        // not the job of the BTSwarmCoordinator
        throw new UnsupportedOperationException("BTSwarmCoordinator.verify() is not implemented.");
    }

    public void reverify() {
        // not the job of the BTSwarmCoordinator
        throw new UnsupportedOperationException("BTSwarmCoordinator.reverify() is not implemented.");

    }

    public long write(Range range, ByteBuffer content) throws IOException {
        // not the job of the BTSwarmCoordinator
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.write(Range, ByteBuffer) is not implemented.");
    }

    public void wrote(Range range) {
        // not the job of the BTSwarmCoordinator
        throw new UnsupportedOperationException(
                "BTSwarmCoordinator.wrote(Range) is not implemented.");
    }

}
