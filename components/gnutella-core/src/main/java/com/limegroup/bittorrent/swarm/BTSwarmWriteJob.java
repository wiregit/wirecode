package com.limegroup.bittorrent.swarm;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.limewire.collection.NECallable;
import org.limewire.swarm.SwarmContent;
import org.limewire.swarm.SwarmWriteJob;
import org.limewire.swarm.SwarmWriteJobControl;

import com.limegroup.bittorrent.BTInterval;
import com.limegroup.bittorrent.BTPiece;
import com.limegroup.bittorrent.disk.TorrentDiskManager;

public class BTSwarmWriteJob implements SwarmWriteJob {

    private final BTInterval bInterval;

    private final TorrentDiskManager torrentDiskManager;

    private final SwarmWriteJobControl callback;

    private final ByteBuffer byteBuffer;

    private final Object writeLock = new Object();

    public BTSwarmWriteJob(BTInterval bInterval, TorrentDiskManager torrentDiskManager,
            SwarmWriteJobControl callback) {
        // TODO support multiple btintervals
        assert bInterval != null;
        assert torrentDiskManager != null;
        assert callback != null;
        /** Range cannot be longer than an integer */
        assert !bInterval.isLong();
        this.bInterval = bInterval;
        this.torrentDiskManager = torrentDiskManager;
        this.callback = callback;
        this.byteBuffer = ByteBuffer.allocate((int) bInterval.getLength());
    }

    public void cancel() {
        // nothing to cancel, we will be relying on the torrent disk manager to
        // do the actual writing
        // This job will just be used to aggregate the data we need

    }

    public long write(SwarmContent content) throws IOException {
        long read = 0;
        synchronized (writeLock) {

            read = content.read(byteBuffer);
            callback.resume();
            if (byteBuffer.remaining() == 0) {
                // piece is done reading
                torrentDiskManager.writeBlock(new NECallable<BTPiece>() {

                    public BTPiece call() {
                        return new BTPiece() {

                            public byte[] getData() {
                                return byteBuffer.array();
                            }

                            public BTInterval getInterval() {
                                return bInterval;
                            }
                        };
                    }

                });

            }
        }
        return read;
    }

}
