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

public class BTSwarmWriteJob implements SwarmWriteJob {

    private final List<BTInterval> pieces;

    private final TorrentDiskManager torrentDiskManager;

    private final SwarmWriteJobControl callback;

    private ByteBuffer byteBuffer;

    private final Object writeLock = new Object();

    private int index = 0;

    private BTInterval piece = null;

    public BTSwarmWriteJob(List<BTInterval> pieces, TorrentDiskManager torrentDiskManager,
            SwarmWriteJobControl callback) {
        this(pieces, torrentDiskManager, callback, 8 * 1024);
    }

    public BTSwarmWriteJob(List<BTInterval> pieces, TorrentDiskManager torrentDiskManager,
            SwarmWriteJobControl callback, int maxBufferSize) {
        assert pieces != null;
        assert torrentDiskManager != null;
        assert callback != null;
        assert maxBufferSize > 0;
        this.pieces = pieces;
        this.torrentDiskManager = torrentDiskManager;
        this.callback = callback;
        this.byteBuffer = null;
    }

    public void cancel() {
        // nothing to cancel, we will be relying on the torrent disk manager to
        // do the actual writing
        // This job will just be used to aggregate the data we need

    }

    public long write(SwarmContent content) throws IOException {
        synchronized (writeLock) {
            if (byteBuffer == null) {
                piece = pieces.get(index++);
                // TODO Use the SwarmWriteJob2 implementation instead
                byteBuffer = ByteBuffer.allocate(piece.get32BitLength());
            }

            long read = 0;
            read = content.read(byteBuffer);
            callback.resume();
            if (byteBuffer.remaining() == 0) {
                // piece is done reading
                final byte[] data = byteBuffer.array();
                byteBuffer = null;
                torrentDiskManager.writeBlock(new NECallable<BTPiece>() {

                    public BTPiece call() {
                        return new BTPiece() {

                            public byte[] getData() {
                                return data;
                            }

                            public BTInterval getInterval() {
                                return piece;
                            }
                        };
                    }

                });

            }
            return read;
        }
    }

}
