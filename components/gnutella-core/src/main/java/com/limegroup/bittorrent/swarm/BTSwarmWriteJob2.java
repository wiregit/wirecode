package com.limegroup.bittorrent.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.limewire.collection.NECallable;
import org.limewire.swarm.SwarmContent;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTPiece;
import com.limegroup.bittorrent.disk.TorrentDiskManager;

public class BTSwarmWriteJob2 implements SwarmWriteJob {

    private final List<BTInterval> pieces;

    private final TorrentDiskManager torrentDiskManager;

    private final SwarmWriteJobControl callback;

    private ByteBuffer byteBuffer;

    private final Object writeLock = new Object();

    private long piecePosition;

    private long pieceLow;

    private int pieceIndex = 0;

    public BTSwarmWriteJob2(List<BTInterval> pieces, TorrentDiskManager torrentDiskManager,
            SwarmWriteJobControl callback) {
        assert pieces != null;
        assert pieces.size() > 0;
        assert torrentDiskManager != null;
        assert callback != null;

        this.pieces = pieces;
        this.torrentDiskManager = torrentDiskManager;
        this.callback = callback;
        this.byteBuffer = null;
        this.piecePosition = pieces.get(0).get32BitLow();
        this.pieceLow = pieces.get(0).get32BitLow();

    }

    public void cancel() {
        // nothing to cancel, we will be relying on the torrent disk manager to
        // do the actual writing
        // This job will just be used to aggregate the data we need

    }

    public long write(SwarmContent content) throws IOException {
        synchronized (writeLock) {

            BTInterval piece = pieces.get(pieceIndex);
            if (byteBuffer == null) {
                int bufferSize = 8 * 1024;
                if (bufferSize + piecePosition > piece.getHigh()) {
                    bufferSize = (int) (piece.get32BitHigh() - piecePosition) + 1;
                }

                byteBuffer = ByteBuffer.allocate(bufferSize);
            }

            long read = content.read(byteBuffer);
            piecePosition += read;
            callback.resume();

            if (byteBuffer.remaining() == 0) {
                // piece is done reading
                final byte[] data = byteBuffer.array();
                byteBuffer = null;
                final int pieceId = piece.getId();

                final long pieceLow = this.pieceLow;
                final long pieceHigh = pieceLow + data.length - 1;

                torrentDiskManager.writeBlock(new NECallable<BTPiece>() {

                    public BTPiece call() {
                        return new BTPiece() {

                            public byte[] getData() {
                                return data;
                            }

                            public BTInterval getInterval() {
                                BTInterval interval = new BTInterval(pieceLow, pieceHigh, pieceId);
                                return interval;
                            }
                        };
                    }

                });

                this.pieceLow = pieceHigh + 1;
                if (piecePosition == piece.getHigh()) {
                    if (pieceIndex + 1 == pieces.size()) {
                        return read;
                    }
                    System.out.println("next peice time");
                    this.pieceLow = pieces.get(++pieceIndex).getLow();
                    piecePosition = pieceLow;
                }
            }
            return read;
        }
    }

}
