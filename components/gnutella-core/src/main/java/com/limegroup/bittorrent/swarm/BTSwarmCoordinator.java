package com.limegroup.bittorrent.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
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
        BTInterval btInterval = createBTInterval(range);
        return new BTSwarmWriteJob(btInterval, torrentDiskManager, callback);
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
        BTInterval leased = torrentDiskManager.leaseRandom(availableRangesBitField, null);

        Range lease = null;
        if (leased != null) {
            long startByte = leased.getBlockId() * btMetaInfo.getPieceLength() + leased.getLow();
            long endByte = startByte + leased.getLength() - 1;
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
        BTInterval oldInterval = createBTInterval(oldLease);
        BTInterval newInterval = createBTInterval(newLease);
        torrentDiskManager.renewLease(oldInterval, newInterval);
        return newLease;
    }

    public void reverify() {
        throw new UnsupportedOperationException("BTSwarmCoordinator.reverify() is not implemented.");

    }

    public void unlease(Range range) {

        // TODO really need to iterate through the range.
        // there will might be multiple peices in reality.
        // change the torrent disk manager to accept either a list of
        // btintervals
        // or a range that will be converted into the BTInterval list.
        BTInterval piece = createBTInterval(range);
        torrentDiskManager.releaseInterval(piece);
    }

    /**
     * 
     * 
     * @param range
     * @return
     */
    private BTInterval createBTInterval(Range range) {
        //TODO this range might really be multiple BTIntervals
        //we need to handle the case when it is
        int pieceLength = btMetaInfo.getPieceLength();
        int pieceNum = (int) Math.floor(range.getLow() / pieceLength);

        long lowByte = range.getLow() - (pieceLength * pieceNum);
        long highByte = lowByte + range.getLength() - 1;
        Range btRange = Range.createRange(lowByte, highByte);
        BTInterval piece = new BTInterval(btRange, pieceNum);
        return piece;
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
